package de.solidblocks.infra.test

import java.lang.annotation.Inherited

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
/*
@ExtendWith(
    SolidblocksTestExtension::class
)*/
@Inherited
annotation class SolidblocksTest1