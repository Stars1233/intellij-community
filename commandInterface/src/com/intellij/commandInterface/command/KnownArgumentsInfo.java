// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.commandInterface.command;

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * In some special cases we have insight about command arguments.
 * In this case we use this class to improve arguments and validation support.
 * Command may have several arguments, while some are optional and some may have many values.
 * Arguments are positional (not named!) by optparse design, so only last argument may be optional but it also may be repeated several times.
 * <p>
 * Examples:
 * <pre>
 *  my_command required_arg1_value required_arg2_value [optinal_arg3_value] [optinal_arg4_value]
 * </pre>
 * Or even like this:
 * <pre>
 *  my_command required_arg1_value [optional_another_arg1_value_1] [optional_another_arg1_value_2] ... [optional_another_arg1_value_N]
 * </pre>.
 * </p>
 *
 * @author Ilya.Kazakevich
 */
public final class KnownArgumentsInfo implements ArgumentsInfo {
  /**
   * List of real arguments.
   */
  private final @NotNull List<Argument> myArguments = new ArrayList<>();
  /**
   * Minimum number of arguments this command requires (actually, number of required arguments)
   */
  private final int myMinArguments;
  /**
   * Maximum number of arguments this command accepts
   * (number of required arguments + num of optional arguments or {@link Integer#MAX_VALUE} if last argument may have infinite
   * number of values)
   */
  private final int myMaxArguments;

  /**
   * For command with fixed number of arguments. In this case all arguments are fixed and required!
   *
   * @param arguments arguments this command have
   */
  public KnownArgumentsInfo(final @NotNull Collection<Argument> arguments) {
    this(arguments, arguments.size(), arguments.size());
  }

  /**
   *
   * @param minArguments minimum arguments required
   * @param maxArguments maximum argument allowed
   * @param arguments arguments
   */
  public KnownArgumentsInfo(final int minArguments, final int maxArguments, final Argument @NotNull ... arguments) {
    this(Arrays.asList(arguments), minArguments, maxArguments);
  }

  /**
   * For commands with infinite number of values last argument accepts (my_command VAL1 VAL2 .. VALN)
   *
   * @param arguments    list of known arguments (last one would be used to accept all residual values)
   * @param minArguments number of required arguments
   */
  public KnownArgumentsInfo(final @NotNull Collection<Argument> arguments,
                            final int minArguments) {
    this(arguments, minArguments, Integer.MAX_VALUE);
  }

  /**
   * For commands with finite number of values last argument accepts.
   *
   * @param arguments    list of known arguments (last one would be used to accept all residual values, but when {@link #myMaxArguments} reached,
   *                     null will be returned)
   * @param minArguments number of required arguments
   * @param maxArguments maximum number of argument values this command accepts
   */
  public KnownArgumentsInfo(final @NotNull Collection<Argument> arguments,
                            final int minArguments,
                            final int maxArguments) {
   assert !arguments.isEmpty(): "At least one argument should be provided";
    myArguments.addAll(arguments);
    myMinArguments = minArguments;
    myMaxArguments = maxArguments;
  }

  @Override
  public @Nullable Pair<Boolean, Argument> getArgument(final int argumentPosition) {
    if (argumentPosition >= myMaxArguments) {
      return null;
    }
    final boolean optional = argumentPosition >= myMinArguments;

    if (myArguments.size() > argumentPosition) {
      return Pair.create(!optional, myArguments.get(argumentPosition));
    }

    // We may need last one
    return Pair.create(false, myArguments.get(myArguments.size() - 1));
  }
}
