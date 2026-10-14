package xfacthd.framedblocks.gametest.util;

import xfacthd.framedblocks.common.data.BlockType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TestedType
{
    BlockType type();
}
