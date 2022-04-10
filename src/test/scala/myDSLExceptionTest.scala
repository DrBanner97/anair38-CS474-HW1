import  org.scalatest.GivenWhenThen
import  org.scalatest.featurespec.AnyFeatureSpec
import  myDSLClassExt.myDSLClassExt.myDSLClassExt.*
import  myDSL.myDSL.Exp.*
import  myDSLClassExt.myDSLClassExt.AccessType
import  myDSLClassExt.myDSLClassExt.ImplementationType

class myDSLExceptionTest extends AnyFeatureSpec with GivenWhenThen {

  Feature("If structure"){
    Scenario("scoping in If condition"){
      ClassDef("subClass1",
        Field("a", AccessType.PUBLIC,Value(23)),
        Constructor(),
        Method("someMethod",
          AccessType.PUBLIC,
          ImplementationType.CONCRETE,
          List(),
          DeclareVar("y", Value(10)),
          If(Check(Var("a"), Value(23)), AnonScope(DeclareVar("z", Value(24))), AnonScope(Assign("y",Value(8)))),
          Assign("z", Value(56))
        )
      ).eval()
      DeclareInstance(ClassDef("subClass1"),"inst1").eval()
      AssignInstance(NewObject(ClassDef("subClass1")), "inst1").eval()
      assertThrows[java.lang.Exception]{
        Instance("inst1", InvokeMethod("someMethod", List())).eval()  //subclass2 instance variable shadows someClassName(parent)'s instance variable x
      }
    }

    Scenario("If branching construct"){
      ClassDef("subClass2",
        Field("x", AccessType.PUBLIC,Value(7)),
        Constructor(),
        Method("m2",
          AccessType.PUBLIC,
          ImplementationType.CONCRETE,
          List(),
          If(Check(Var("x"), Value(1)),
            AnonScope(Assign("x", Value(10))),
            AnonScope(Assign("x", CreateSet(Value(3),Value(4),Value(5)))),
          )
        )
      ).eval()
      DeclareInstance(ClassDef("subClass2"),"inst3").eval()
      AssignInstance(NewObject(ClassDef("subClass2")), "inst3").eval()
      Instance("inst3", InvokeMethod("m2", List())).eval()  //subclass2 instance variable shadows someClassName(parent)'s instance variable x
      assert(Instance("inst3", GetField("x")).eval() == Set(3,4,5))

    }


  }

  Feature("Exception handling"){

    Scenario("scoping in Exception handling"){
      ExceptionClassDef("someExceptionName", Field("message", AccessType.PUBLIC)).eval()
      ClassDef("subClass2",
        Field("x", AccessType.PUBLIC,Value(7)),
        Constructor(),
        Method("m2",
          AccessType.PUBLIC,
          ImplementationType.CONCRETE,
          List(),
          CatchException("someExceptionName",
            Catch("exceptionVariable", Value(1)),
            DeclareVar("z", Value(7)),
            Assign("x", Value(10))
          ),
          Var("z")
        )
      ).eval()
      DeclareInstance(ClassDef("subClass2"),"inst3").eval()
      AssignInstance(NewObject(ClassDef("subClass2")), "inst3").eval()

      assertThrows[java.lang.Exception]{
        Instance("inst3", InvokeMethod("m2", List())).eval()
      }
    }

    Scenario("Error message catching"){
      assert(CatchException("someExceptionName",
        Catch("exceptionVariable", Instance("exceptionVariable", GetField("message"))),
        DeclareVar("x", Value(7)),
        ThrowException(ClassDef("someExceptionName"), AssignField("message", "forced error thrown")),
        Assign("x", Value(10))
      ).eval() == "forced error thrown")
    }

    Scenario("Inability to catch unhandled exception"){
      ExceptionClassDef("someExceptionName1", Field("message", AccessType.PUBLIC)).eval()
      assertThrows[java.lang.Exception]{
        CatchException("someExceptionName",
          Catch("exceptionVariable", Instance("exceptionVariable", GetField("message"))),
          DeclareVar("x", Value(7)),
          ThrowException(ClassDef("someExceptionName1"), AssignField("message", "forced error for someExceptionName1")),
          Assign("x", Value(10))
        ).eval()
      }
    }
  }
}
