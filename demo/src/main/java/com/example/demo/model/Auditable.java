package com.example.demo.model;
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)

public @interface Auditable {
    String action();
    String entity() default "";
}
//solo registra cuando la accion es exitosa por ahora