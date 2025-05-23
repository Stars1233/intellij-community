// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.lang.html.structureView;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.containers.SortedList;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;

// Algorithm described on https://www.w3.org/TR/html51/sections.html#creating-an-outline
// One of the implementations: http://hoyois.github.com/html5outliner/ (https://github.com/hoyois/html5outliner)
final class Html5SectionsProcessor {
  private static class SectionHolder {
    private final XmlTag myTag;
    private final LinkedList<Section> myChildren = new LinkedList<>();

    private SectionHolder(final XmlTag tag) {
      myTag = tag;
    }

    public void addChildSection(final Section section) {
      myChildren.add(section);
    }

    public LinkedList<Section> getChildren() {
      return myChildren;
    }

    public XmlTag getTag() {
      return myTag;
    }
  }

  private static class Section extends SectionHolder {
    private Section myParent = null;
    private XmlTag myHeader = null;

    Section(final XmlTag tag) {
      super(tag);
    }

    @Override
    public void addChildSection(final Section section) {
      section.myParent = this;
      super.addChildSection(section);
    }

    public XmlTag getHeader() {
      return myHeader;
    }

    public void setHeader(final XmlTag header) {
      myHeader = header;
    }

    public Section getParent() {
      return myParent;
    }
  }

  private static final String[] SECTIONING_ROOT_ELEMENTS = {"blockquote", "body", "details", "fieldset", "figure", "td"};
  private static final String[] SECTIONING_CONTENT_ELEMENTS = {"article", "aside", "nav", "section"};
  private static final String[] HEADER_ELEMENTS = {"h1", "h2", "h3", "h4", "h5", "h6"};
  private static final String HGROUP_ELEMENT = "hgroup";

  private final Collection<SectionHolder> myRootSectionHolders = new SortedList<>(
    Comparator.comparingInt(holder -> holder.getTag().getTextRange().getStartOffset()));

  private SectionHolder myCurrentOutlinee = null;
  private Section myCurrentSection = null;
  private final Stack<SectionHolder> myStack = new Stack<>();

  public static Collection<Html5SectionTreeElement> processAndGetRootSections(final XmlTag rootTag) {
    final Html5SectionsProcessor processor = new Html5SectionsProcessor();
    processRecursively(rootTag, processor);
    return processor.getRootSections();
  }

  private static void processRecursively(final XmlTag tag, final Html5SectionsProcessor processor) {
    if (tag.getAttribute("hidden") != null) return;

    processor.tagEntered(tag);

    if (!isHeader(tag)) {
      for (final XmlTag subTag : tag.getSubTags()) {
        processRecursively(subTag, processor);
      }
    }

    processor.tagExited(tag);
  }

  private void tagEntered(final XmlTag tag) {
    if (isSectioningContentElement(tag) || isSectioningRootElement(tag)) {
      if (myCurrentOutlinee != null) {
        myStack.push(myCurrentOutlinee);
      }

      myCurrentOutlinee = new SectionHolder(tag);
      myCurrentSection = new Section(tag);
      myCurrentOutlinee.addChildSection(myCurrentSection);
    }
    else if (myCurrentOutlinee == null) {
      // do nothing
    }
    else if (isHeader(tag)) {
      if (myCurrentSection.getHeader() == null) {
        myCurrentSection.setHeader(tag);
      }
      else if (myCurrentOutlinee.getChildren().getLast().getHeader() == null ||
               compareHeaderRanks(tag, myCurrentOutlinee.getChildren().getLast().getHeader()) >= 0) {
        myCurrentSection = new Section(tag);
        myCurrentSection.setHeader(tag);
        myCurrentOutlinee.addChildSection(myCurrentSection);
      }
      else {
        Section candidateSection = myCurrentSection;
        do {
          if (compareHeaderRanks(tag, candidateSection.getHeader()) < 0) {
            myCurrentSection = new Section(tag);
            myCurrentSection.setHeader(tag);
            candidateSection.addChildSection(myCurrentSection);
            break;
          }
          candidateSection = candidateSection.getParent();
        }
        while (true);
      }
      //myStack.push(); not needed, because our iterator doesn't enter hidden elements
    }
  }

  private void tagExited(final XmlTag tag) {
    if (!myStack.isEmpty() && myStack.peek().getTag() == tag) {
      assert false;
    }
    else if (!myStack.isEmpty() && isHeader(tag)) {
      // do nothing
    }
    else if (!myStack.isEmpty() && isSectioningContentElement(tag)) {
      final SectionHolder exitedSectioningContent = myCurrentOutlinee;
      assert exitedSectioningContent.getTag() == tag;

      myCurrentOutlinee = myStack.pop();
      myCurrentSection = myCurrentOutlinee.getChildren().getLast();

      for (Section section : exitedSectioningContent.getChildren()) {
        myCurrentSection.addChildSection(section);
      }
    }
    else if (!myStack.isEmpty() && isSectioningRootElement(tag)) {
      final SectionHolder exitedSectioningRoot = myCurrentOutlinee;
      assert exitedSectioningRoot.getTag() == tag;
      myRootSectionHolders.add(exitedSectioningRoot);

      myCurrentOutlinee = myStack.pop();

      myCurrentSection = myCurrentOutlinee.getChildren().getLast();
      while (!myCurrentSection.getChildren().isEmpty()) {
        myCurrentSection = myCurrentSection.getChildren().getLast();
      }
    }
    else if (isSectioningContentElement(tag) || isSectioningRootElement(tag)) {
      assert myStack.isEmpty();

      assert myCurrentOutlinee.getTag() == tag;
      myRootSectionHolders.add(myCurrentOutlinee);

      // reset algorithm
      myCurrentOutlinee = null;
      myCurrentSection = null;
    }
  }

  private Collection<Html5SectionTreeElement> getRootSections() {
    final Collection<Html5SectionTreeElement> result = new ArrayList<>();
    for (SectionHolder sectionHolder : myRootSectionHolders) {
      for (Section section : sectionHolder.getChildren()) {
        if ("td".equalsIgnoreCase(section.getTag().getName()) && section.getHeader() == null && section.getChildren().isEmpty()) {
          continue;
        }
        result.add(createHtml5SectionTreeElement(section));
      }
    }
    return result;
  }

  private static Html5SectionTreeElement createHtml5SectionTreeElement(final Section section) {
    return new Html5SectionTreeElement(section.getTag(),
                                       createChildrenComputable(section.getChildren()),
                                       getHeaderText(section.getHeader()));
  }

  private static Computable<Collection<StructureViewTreeElement>> createChildrenComputable(final Collection<Section> children) {
    return () -> {
      final Collection<StructureViewTreeElement> result = new ArrayList<>();
      for (Section section : children) {
        result.add(createHtml5SectionTreeElement(section));
      }
      return result;
    };
  }

  private static String getHeaderText(final @Nullable XmlTag header) {
    if (header == null) return null;

    final StringBuilder buf = new StringBuilder();

    if (HGROUP_ELEMENT.equalsIgnoreCase(header.getLocalName())) {
      for (XmlTag subTag : header.getSubTags()) {
        if (ArrayUtil.contains(StringUtil.toLowerCase(subTag.getLocalName()), HEADER_ELEMENTS)) {
          if (!buf.isEmpty()) {
            buf.append(" ");
          }
          appendTextRecursively(subTag, buf, HtmlTagTreeElement.MAX_TEXT_LENGTH * 2);
        }
      }
    }
    else {
      appendTextRecursively(header, buf, HtmlTagTreeElement.MAX_TEXT_LENGTH * 2);
    }

    return buf.toString();
  }

  private static void appendTextRecursively(final XmlTag tag, final StringBuilder buf, final int maximumTextLength) {
    if (buf.length() >= maximumTextLength) return;

    final String text = tag.getValue().getTrimmedText();
    if (!text.isEmpty()) {
      buf.append(text);
    }
    else {
      for (XmlTag subTag : tag.getSubTags()) {
        appendTextRecursively(subTag, buf, maximumTextLength);
      }
    }
  }

  private static boolean isSectioningRootElement(final XmlTag tag) {
    return ArrayUtil.contains(StringUtil.toLowerCase(tag.getLocalName()), SECTIONING_ROOT_ELEMENTS);
  }

  private static boolean isSectioningContentElement(final XmlTag tag) {
    return ArrayUtil.contains(StringUtil.toLowerCase(tag.getLocalName()), SECTIONING_CONTENT_ELEMENTS);
  }

  private static boolean isHeader(final XmlTag tag) {
    return ArrayUtil.contains(StringUtil.toLowerCase(tag.getLocalName()), HEADER_ELEMENTS) || HGROUP_ELEMENT.equalsIgnoreCase(tag.getLocalName());
  }

  private static int compareHeaderRanks(final @NotNull XmlTag header1, final @NotNull XmlTag header2) {
    return getHeaderRank(header2) - getHeaderRank(header1);
  }

  private static int getHeaderRank(final XmlTag header) {
    if (HGROUP_ELEMENT.equalsIgnoreCase(header.getLocalName())) {
      int minIndex = HEADER_ELEMENTS.length;

      for (XmlTag subTag : header.getSubTags()) {
        int index = ArrayUtilRt.indexOf(HEADER_ELEMENTS, StringUtil.toLowerCase(subTag.getLocalName()), 0, HEADER_ELEMENTS.length);
        if (index < minIndex) {
          minIndex = index;
          if (minIndex == 0) break;
        }
      }

      if (minIndex == HEADER_ELEMENTS.length) {
        // no headers is equivalent to <h1>
        minIndex = 0;
      }

      return minIndex + 1;
    }

    int index = ArrayUtilRt.indexOf(HEADER_ELEMENTS, StringUtil.toLowerCase(header.getLocalName()), 0, HEADER_ELEMENTS.length);
    if (index < 0) throw new IllegalArgumentException(header.getName());
    return index + 1;
  }
}
