// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.util.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Pair;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.html.SummaryView;
import com.intellij.util.ui.html.UtilsKt;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.*;
import javax.swing.text.html.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;

import static com.intellij.util.ui.html.UtilsKt.reapplyCss;

@ApiStatus.NonExtendable
public class JBHtmlEditorKit extends HTMLEditorKit {
  private static final Logger LOG = Logger.getInstance(JBHtmlEditorKit.class);

  private final @NotNull ViewFactory myViewFactory;
  private final @NotNull StyleSheet myStyle;

  private final @NotNull HTMLEditorKit.LinkController myLinkController = new MouseExitSupportLinkController();
  private final @NotNull HyperlinkListener myHyperlinkListener = new LinkUnderlineListener();
  private final @NotNull JBHtmlEditorKit.DetailsSummaryController myDetailsSummaryController = new DetailsSummaryController();
  private final boolean myDisableLinkedCss;
  private boolean myUnderlineHoveredHyperlink = true;

  private @Nullable CSSFontResolver myFontResolver;

  /**
   * @deprecated use {@link HTMLEditorKitBuilder}
   */
  @Deprecated
  public JBHtmlEditorKit() {
    this(true);
  }

  /**
   * @deprecated use {@link HTMLEditorKitBuilder}
   */
  @Deprecated
  public JBHtmlEditorKit(boolean noGapsBetweenParagraphs) {
    this(ExtendableHTMLViewFactory.DEFAULT, StyleSheetUtil.getDefaultStyleSheet(), false);
    if (noGapsBetweenParagraphs) {
      getStyleSheet().addStyleSheet(StyleSheetUtil.INSTANCE.getNO_GAPS_BETWEEN_PARAGRAPHS_STYLE());
    }
  }

  /**
   * @param viewFactory      view factory for this kit, generally should be static
   * @param defaultStyle     css stylesheet to be used in all documents created by {@link #createDefaultDocument()}
   * @param disableLinkedCss disables loading of linked CSS (from URL referenced in &lt;link&gt; HTML tags). JEditorPane does this loading
   *                         synchronously during {@link JEditorPane#setText(String)} operation (usually invoked in EDT).
   */
  JBHtmlEditorKit(@NotNull ViewFactory viewFactory,
                  @NotNull StyleSheet defaultStyle,
                  boolean disableLinkedCss) {
    myViewFactory = viewFactory;
    myDisableLinkedCss = disableLinkedCss;
    myStyle = defaultStyle;
  }

  /**
   * Toggle whether hyperlinks are underlined when hovered.
   * <p>
   * This is useful if another implementation needs to apply a different style
   * to hyperlinks when hovered (this can be done by adding a {@link HyperlinkListener}
   * to the {@link JEditorPane}).
   */
  public void setUnderlineHoveredHyperlink(boolean underlineHoveredHyperlink) {
    myUnderlineHoveredHyperlink = underlineHoveredHyperlink;
  }

  /**
   * This allows to impact resolution of fonts from CSS attributes of text
   */
  public void setFontResolver(@Nullable CSSFontResolver fontResolver) {
    myFontResolver = fontResolver;
  }

  @Override
  public StyleSheet getStyleSheet() {
    return myStyle;
  }

  /**
   * Will not work as one might expect it to.
   * To override default style you should use the provided constructor.
   */
  @Override
  public void setStyleSheet(StyleSheet style) {
    //prevent setting the global style
  }

  @Override
  public Document createDefaultDocument() {
    StyleSheet styles = getStyleSheet();
    StyleSheet ss = new StyleSheetCompressionThreshold();
    ss.addStyleSheet(styles);

    HTMLDocument doc = new JBHtmlDocument(ss);
    doc.setParser(getParser());
    doc.setAsynchronousLoadPriority(4);
    doc.setTokenThreshold(100);
    return doc;
  }

  @Override
  public Cursor getDefaultCursor() {
    return null;
  }

  @Override
  public void install(final JEditorPane pane) {
    super.install(pane);
    // JEditorPane.HONOR_DISPLAY_PROPERTIES must be set after HTMLEditorKit is completely installed
    pane.addPropertyChangeListener("editorKit", new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent e) {
        // In case JBUI user scale factor changes, the font will be auto-updated by BasicTextUI.installUI()
        // with a font of the properly scaled size. And is then propagated to CSS, making HTML text scale dynamically.

        // The default JEditorPane's font is the label font, seems there's no need to reset it here.
        // If the default font is overridden, more so we should not reset it.
        // However, if the new font is not UIResource - it won't be auto-scaled.
        // [tav] dodo: remove the next two lines in case there're no regressions
        //Font font = getLabelFont();
        //pane.setFont(font);

        // let CSS font properties inherit from the pane's font
        pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        pane.removePropertyChangeListener(this);
      }
    });
    if (myUnderlineHoveredHyperlink) {
      pane.addHyperlinkListener(myHyperlinkListener);
    }
    pane.addMouseListener(myDetailsSummaryController);

    java.util.List<LinkController> listeners1 = filterLinkControllerListeners(pane.getMouseListeners());
    java.util.List<LinkController> listeners2 = filterLinkControllerListeners(pane.getMouseMotionListeners());
    // replace just the original listener
    if (listeners1.size() == 1 && listeners1.equals(listeners2)) {
      LinkController oldLinkController = listeners1.get(0);
      pane.removeMouseListener(oldLinkController);
      pane.removeMouseMotionListener(oldLinkController);
      pane.addMouseListener(myLinkController);
      pane.addMouseMotionListener(myLinkController);
    }
  }

  @Override
  public ViewFactory getViewFactory() {
    return myViewFactory;
  }

  private static @Unmodifiable @NotNull List<LinkController> filterLinkControllerListeners(Object @NotNull [] listeners) {
    return ContainerUtil.mapNotNull(listeners, o -> ObjectUtils.tryCast(o, LinkController.class));
  }

  @Override
  public void deinstall(@NotNull JEditorPane c) {
    c.removeMouseListener(myLinkController);
    c.removeMouseMotionListener(myLinkController);
    c.removeHyperlinkListener(myHyperlinkListener);
    super.deinstall(c);
  }

  // This needs to be a static class to avoid memory leaks.
  // It's because StyleSheet instance gets leaked into parent (global) StyleSheet
  // due to JDK implementation nuances (see javax.swing.text.html.CSS#getStyleSheet)
  private static final class StyleSheetCompressionThreshold extends StyleSheet {
    @Override
    protected int getCompressionThreshold() {
      return -1;
    }
  }

  // Workaround for https://bugs.openjdk.org/browse/JDK-8202529
  private static final class MouseExitSupportLinkController extends HTMLEditorKit.LinkController {
    @Override
    public void mouseExited(@NotNull MouseEvent e) {
      mouseMoved(new MouseEvent(e.getComponent(), e.getID(), e.getWhen(), e.getModifiersEx(), -1, -1, e.getClickCount(), e.isPopupTrigger(),
                                e.getButton()));
    }
  }

  private static final class DetailsSummaryController extends MouseAdapter {
    @Override
    public void mouseClicked(MouseEvent e) {
      JEditorPane editor = (JEditorPane) e.getSource();

      if (! editor.isEditable() && editor.isEnabled() &&
          SwingUtilities.isLeftMouseButton(e)) {
        Point pt = new Point(e.getX(), e.getY());
        int pos = editor.viewToModel(pt);
        if (pos >= 0) {
          clickSummary(pos, editor);
        }
      }
    }

    private static void clickSummary(int pos, JEditorPane html) {
      Document doc = html.getDocument();
      if (doc instanceof HTMLDocument hdoc) {
        Element e = hdoc.getCharacterElement(pos);
        while (e != null) {
          String name = e.getName();
          if (name.equals("a")) break;
          if (name.equals("summary")) {
            AttributeSet attributes = e.getAttributes();
            Object attribute = attributes.getAttribute(UtilsKt.getHTML_Tag_DETAILS());
            if (attribute instanceof MutableAttributeSet a) {
              a.addAttribute(SummaryView.EXPANDED, a.getAttribute(SummaryView.EXPANDED) != Boolean.TRUE);
            }
            var rootView = html.getUI().getRootView(html);
            reapplyCss(rootView);

            // Force view to be relayouted
            rootView.setSize(1,1);

            // Force the JEditorPane to be relayouted
            html.revalidate();
            html.doLayout();
            return;
          }
          e = e.getParentElement();
        }
      }
    }
  }

  /**
   * @see HTMLEditorKitBuilder
   * @deprecated in favor of {@link ExtendableHTMLViewFactory}
   */
  @Deprecated(forRemoval = true)
  public static class JBHtmlFactory extends HTMLFactory {

    private final ViewFactory myDelegate = ExtendableHTMLViewFactory.DEFAULT;

    @Override
    public View create(Element elem) {
      return myDelegate.create(elem);
    }
  }

  @ApiStatus.Internal
  public final class JBHtmlDocument extends HTMLDocument {
    private JBHtmlDocument(StyleSheet styles) {
      super(styles);
      TextLayoutUtil.disableTextLayoutIfNeeded(this);
    }

    @Override
    public Font getFont(AttributeSet a) {
      Font font = super.getFont(a);
      return myFontResolver == null ? font : myFontResolver.getFont(font, a);
    }

    @Override
    public ParserCallback getReader(int pos) {
      Object desc = getProperty(StreamDescriptionProperty);
      if (desc instanceof URL) {
        setBase((URL)desc);
      }
      ParserCallback reader = new JBHtmlReader(pos);
      return myDisableLinkedCss ? new CallbackWrapper(reader) : reader;
    }

    @Override
    public ParserCallback getReader(int pos, int popDepth, int pushDepth, HTML.Tag insertTag) {
      Object desc = getProperty(StreamDescriptionProperty);
      if (desc instanceof URL) {
        setBase((URL)desc);
      }
      HTMLReader reader = new JBHtmlReader(pos, popDepth, pushDepth, insertTag);
      return myDisableLinkedCss ? new CallbackWrapper(reader) : reader;
    }

    public void tryRunUnderWriteLock(Runnable runnable) {
      if (getCurrentWriter() == Thread.currentThread()) {
        runnable.run();
      }
      else {
        try {
          writeLock();
        }
        catch (IllegalStateException e) {
          // ignore, wrong thread
          return;
        }
        try {
          runnable.run();
        }
        finally {
          writeUnlock();
        }
      }
    }

    private final class JBHtmlReader extends HTMLReader {

      private JBHtmlReader(int offset) {
        super(offset);
        registerAdditionalTags();
      }

      private JBHtmlReader(int offset, int popDepth, int pushDepth, HTML.Tag insertTag) {
        super(offset, popDepth, pushDepth, insertTag);
        registerAdditionalTags();
      }

      private void registerAdditionalTags() {
        TagAction ba = new BlockAction();
        for (HTML.Tag tag: UtilsKt.getHTML_Tag_CUSTOM_BLOCK_TAGS()) {
          registerTag(tag, ba);
        }
      }

      @Override
      protected void addSpecialElement(HTML.Tag t, MutableAttributeSet a) {
        int lastSize = parseBuffer.size();
        super.addSpecialElement(t, a);
        if (lastSize != parseBuffer.size()) {
          if (t == HTML.Tag.BR) {
            var elementSpec = parseBuffer.lastElement();
            parseBuffer.set(parseBuffer.size() - 1, new ElementSpec(
              elementSpec.getAttributes(), ElementSpec.ContentType, new char[]{'\n'}, 0, 1));
          }
          else if ("wbr".equals(t.toString())) {
            var elementSpec = parseBuffer.lastElement();
            // Swing HTML control does not accept elements with no text.
            // Use zero-width space. It needs to be removed in `getSelectedText()`
            // to avoid this char showing up while copy/pasting.
            parseBuffer.set(parseBuffer.size() - 1, new ElementSpec(
              elementSpec.getAttributes(), ElementSpec.ContentType, new char[]{'\u200B'}, 0, 1));
          }
        }
      }
    }

    private static final class CallbackWrapper extends ParserCallback {
      private final ParserCallback delegate;
      private int depth;

      private CallbackWrapper(ParserCallback delegate) { this.delegate = delegate; }

      @Override
      public void flush() throws BadLocationException {
        delegate.flush();
      }

      @Override
      public void handleText(char[] data, int pos) {
        if (depth > 0) return;
        delegate.handleText(data, pos);
      }

      @Override
      public void handleComment(char[] data, int pos) {
        if (depth > 0) return;
        delegate.handleComment(data, pos);
      }

      @Override
      public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
        if (t == HTML.Tag.LINK) depth++;
        if (depth > 0) return;
        delegate.handleStartTag(t, a, pos);
      }

      @Override
      public void handleEndTag(HTML.Tag t, int pos) {
        if (t == HTML.Tag.LINK) depth--;
        LOG.assertTrue(depth >= 0);
        if (depth > 0) return;
        delegate.handleEndTag(t, pos);
      }

      @Override
      public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
        if (t == HTML.Tag.LINK) return;
        delegate.handleSimpleTag(t, a, pos);
      }

      @Override
      public void handleError(String errorMsg, int pos) {
        delegate.handleError(errorMsg, pos);
      }

      @Override
      public void handleEndOfLineString(String eol) {
        delegate.handleEndOfLineString(eol);
      }
    }
  }

  private static final class LinkUnderlineListener implements HyperlinkListener {
    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
      Element element = e.getSourceElement();
      if (element == null) return;
      if (element.getName().equals("img")) return;

      if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
        setUnderlined(true, element);
      }
      else if (e.getEventType() == HyperlinkEvent.EventType.EXITED) {
        setUnderlined(false, element);
      }
    }

    private static void setUnderlined(boolean underlined, @NotNull Element element) {
      AttributeSet attributes = element.getAttributes();
      Object attribute = attributes.getAttribute(HTML.Tag.A);
      if (attribute instanceof MutableAttributeSet a) {

        Object href = a.getAttribute(HTML.Attribute.HREF);
        Pair<Integer, Integer> aRange = findRangeOfParentTagWithGivenAttribute(element, href, HTML.Tag.A, HTML.Attribute.HREF);

        a.addAttribute(CSS.Attribute.TEXT_DECORATION, underlined ? "underline" : "none");
        ((StyledDocument)element.getDocument()).setCharacterAttributes(aRange.first, aRange.second - aRange.first, a, false);
      }
    }

    /**
     * There was a bug that if the anchor tag contained some span block, e.g.
     * {@code <a><span>Objects.</span><span>equals()</span></a>} then only one block
     * was underlined on mouse hover.
     *
     * <p>That was due to the receiver of the {@code HyperlinkEvent} was only one of child blocks
     * and we need to properly find the range occupied by the whole parent.</p>
     */
    @SuppressWarnings("SameParameterValue")
    private static @NotNull Pair<Integer, Integer> findRangeOfParentTagWithGivenAttribute(
      @NotNull Element element,
      Object elementAttributeValue,
      @NotNull HTML.Tag tag,
      @NotNull HTML.Attribute attribute
    ) {
      HtmlIteratorWrapper anchorTagIterator = new HtmlIteratorWrapper(((HTMLDocument)element.getDocument()).getIterator(tag));
      return StreamSupport.stream(anchorTagIterator.spliterator(), false)
        .filter(it -> Objects.equals(it.getAttributes().getAttribute(attribute), elementAttributeValue))
        .filter(it -> it.getStartOffset() <= element.getStartOffset() && element.getStartOffset() <= it.getEndOffset())
        .map(it -> new Pair<>(it.getStartOffset(), it.getEndOffset()))
        .findFirst()
        .orElse(new Pair<>(element.getStartOffset(), element.getEndOffset()));
    }

    /**
     * Implements {@code java.lang.Iterable} for {@code HTMLDocument.Iterator}
     */
    private static final class HtmlIteratorWrapper implements Iterable<HTMLDocument.Iterator> {

      private final @NotNull HTMLDocument.Iterator myDelegate;

      private HtmlIteratorWrapper(@NotNull HTMLDocument.Iterator delegate) {
        myDelegate = delegate;
      }

      @Override
      public @NotNull Iterator<HTMLDocument.Iterator> iterator() {
        return new Iterator<>() {
          @Override
          public boolean hasNext() {
            return myDelegate.isValid();
          }

          @Override
          public HTMLDocument.Iterator next() {
            final AttributeSet attributeSet = myDelegate.getAttributes();
            final int startOffset = myDelegate.getStartOffset();
            final int endOffset = myDelegate.getEndOffset();
            final HTML.Tag tag = myDelegate.getTag();
            HTMLDocument.Iterator current = new HTMLDocument.Iterator() {
              @Override
              public AttributeSet getAttributes() {
                return attributeSet;
              }

              @Override
              public int getStartOffset() {
                return startOffset;
              }

              @Override
              public int getEndOffset() {
                return endOffset;
              }

              @Override
              public HTML.Tag getTag() {
                return tag;
              }

              @Override
              public void next() {
                throw new IllegalStateException("Must not be called");
              }

              @Override
              public boolean isValid() {
                throw new IllegalStateException("Must not be called");
              }
            };
            myDelegate.next();
            return current;
          }
        };
      }
    }
  }
}
