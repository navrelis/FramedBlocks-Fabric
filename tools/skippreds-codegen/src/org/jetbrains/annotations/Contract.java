package org.jetbrains.annotations;

/** Minimal stand-in for the JetBrains annotation so the generator compiles without external dependencies. */
public @interface Contract
{
    String value() default "";
}
