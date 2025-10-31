package vn.pmgteam.luna;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface UiPackFiles {
    String layoutClass() default "ui.DefaultLayout";
}
