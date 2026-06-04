package com.flowdeck.backend.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** 애플리케이션 레이어 의존성과 네이밍 규칙이 아키텍처 제약을 지키는지 검증한다. */
@AnalyzeClasses(
    packages = "com.flowdeck.backend",
    importOptions = {ImportOption.DoNotIncludeTests.class})
class ArchitectureRulesTest {

  @ArchTest
  static final ArchRule CONTROLLER_CLASSES_SHOULD_BE_NAMED_CONSISTENTLY =
      classes()
          .that()
          .resideInAPackage("..controller..")
          .should()
          .haveSimpleNameEndingWith("Controller")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule SERVICE_CLASSES_SHOULD_BE_NAMED_CONSISTENTLY =
      classes()
          .that()
          .resideInAPackage("..service..")
          .should()
          .haveSimpleNameEndingWith("Service")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule REPOSITORY_CLASSES_SHOULD_BE_NAMED_CONSISTENTLY =
      classes()
          .that()
          .resideInAPackage("..repository..")
          .should()
          .haveSimpleNameEndingWith("Repository")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule CONTROLLERS_SHOULD_NOT_DEPEND_ON_REPOSITORIES =
      noClasses()
          .that()
          .resideInAPackage("..controller..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..repository..")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule SERVICES_SHOULD_NOT_DEPEND_ON_CONTROLLERS =
      noClasses()
          .that()
          .resideInAPackage("..service..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..controller..")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule REPOSITORIES_SHOULD_NOT_DEPEND_ON_WEB_OR_SERVICE_LAYERS =
      noClasses()
          .that()
          .resideInAPackage("..repository..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..controller..", "..service..")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule TRANSACTIONAL_METHODS_SHOULD_STAY_IN_SERVICES =
      methods()
          .that()
          .areAnnotatedWith(Transactional.class)
          .should()
          .beDeclaredInClassesThat()
          .resideInAPackage("..service..")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule TRANSACTIONAL_CLASSES_SHOULD_STAY_IN_SERVICES =
      classes()
          .that()
          .areAnnotatedWith(Transactional.class)
          .should()
          .resideInAPackage("..service..")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule FIELD_INJECTION_IS_NOT_ALLOWED =
      noFields().should().beAnnotatedWith(Autowired.class);
}
