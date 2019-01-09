package com.github.matek2305.djamoe

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import org.scalatest.{FlatSpecLike, Suite}

class PackageStructureValidationSpec extends Suite with FlatSpecLike {

  private val classes = new ClassFileImporter()
    .importPackages(this.getClass.getPackage.getName)

  "package structure" should "have no dependency from domain to application" in {
    noClasses()
      .that().resideInAPackage("..domain..")
      .should().accessClassesThat().resideInAPackage("..app..")
        .check(classes)
  }

  it should "have no dependency from domain to akka framework" in {
    noClasses()
      .that().resideInAPackage("..domain..")
      .should().accessClassesThat().resideInAPackage("..akka..")
      .check(classes)
  }

  it should "have no circular dependencies between packages" in {
    slices().matching("..(**)..").should().beFreeOfCycles().check(classes)
  }
}
