package com.github.matek2305.djamoe

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.scalatest.{FlatSpecLike, Suite}

class PackageStructureValidationSpec extends Suite with FlatSpecLike {

  private val classes: JavaClasses = new ClassFileImporter()
    .importPackages("com.github.matek2305.djamoe")

  "PackageStructureValidation" should "check no dependency from domain to application" in {
    val rule = noClasses().that().resideInAPackage("..domain..")
      .should().accessClassesThat().resideInAPackage("..app..")

    rule.check(classes)
  }

  it should "check no dependency from domain to akka framework" in {
    val rule = noClasses().that().resideInAPackage("..domain..")
      .should().accessClassesThat().resideInAPackage("..akka..")

    rule.check(classes)
  }
}
