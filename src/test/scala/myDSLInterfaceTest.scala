import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec
import  myDSLClassExt.myDSLClassExt.myDSLClassExt.*
import  myDSLClassExt.myDSLClassExt.AccessType
import  myDSLClassExt.myDSLClassExt.ImplementationType
import myDSL.myDSL.Exp.*
import java.util.NoSuchElementException



class myDSLInterfaceTest extends AnyFeatureSpec with GivenWhenThen {

  Feature("Interface creation"){

    Scenario("no private/protected fields in Interface"){

      assertThrows[java.lang.Exception]{
        InterfaceDef("Interface1",
          Field("a", AccessType.PRIVATE, Value(10)),
        ).eval()

      }

      assertThrows[java.lang.Exception]{
        InterfaceDef("Interface1",
          Field("a", AccessType.PROTECTED, Value(10)),
        ).eval()

      }
    }

//    Scenario("value expected for fields in interfaces"){
//      assertThrows[java.lang.Exception]{
//        InterfaceDef("Interface1",
//          Field("a", AccessType.PUBLIC),
//        ).eval()
//
//      }
//
//    }

    Scenario("abstract methods cannot have body"){

      assertThrows[java.lang.Exception]{
        InterfaceDef("Interface1",
          Field("a", AccessType.PUBLIC, Value(10)),
          Method("method1", AccessType.PUBLIC, ImplementationType.ABSTRACT, List(), Value(1))
        ).eval()

      }
    }

    Scenario("interface methods cannot be concrete"){

      assertThrows[java.lang.Exception]{
        InterfaceDef("Interface1",
          Field("a", AccessType.PUBLIC, Value(10)),
          Method("method1", AccessType.PUBLIC, ImplementationType.CONCRETE, List())
        ).eval()

      }
    }

    Scenario("interfaces cannot have constructors"){
      assertThrows[java.lang.Exception]{
        InterfaceDef("Interface1",
          Constructor(Value("1"))
        ).eval()
      }
    }
  }

  Feature("interface inheritance"){
    Scenario("inherited fields"){
      InterfaceDef("someInterface1", Field("a", AccessType.PUBLIC, Value(10))) Extends
        InterfaceDef("superInterface1", Field("b", AccessType.PUBLIC,Value(120)))
      assert(Interface("someInterface1", GetField("b")).eval() == 120)
    }

    Scenario("cyclic composition should throw error"){
      assertThrows[java.lang.Exception]{
        InterfaceDef("someInterface1", Field("a", AccessType.PUBLIC, Value(10))).eval()
        InterfaceDef("someInterface2", Field("b", AccessType.PUBLIC, Value(20))) Extends
          InterfaceDef("someInterface1")
        InterfaceDef("someInterface1") Extends InterfaceDef("someInterface2")
      }
    }

  }


  Feature("interface implements"){
    Scenario("multiple interfaces with duplicate fields"){
      assertThrows[java.lang.Exception]{
        ClassDef("someClassName", Field("a", AccessType.PUBLIC, Value(3))) Implements
          List(InterfaceDef("someInterface1", Field("ar", AccessType.PUBLIC, Value(10))),
            InterfaceDef("someInterface2", Field("ar", AccessType.PUBLIC, Value(10))))
      }
    }

    Scenario("class instance using fields from implemented interfaces"){
      InterfaceDef("someInterface1", Field("a", AccessType.PUBLIC, Value(20))).eval()
      ClassDef("someClassName1",
        Field("y", AccessType.PRIVATE, Value(47)),
        Constructor(Value(1))
      ).eval()
      ClassDef("someClassName1") Implements List(InterfaceDef("someInterface1"))

      DeclareInstance(ClassDef("someClassName1"),"inst1").eval()
      AssignInstance(NewObject(ClassDef("someClassName1")), "inst1").eval()

      assert(Instance("inst1", GetField("a")).eval() == 20)
    }

    Scenario("prioritizing inherited fields over implemented interface fields"){
      ClassDef("someClassName",
        Field("a", AccessType.PUBLIC, Value(47)),
        Constructor(Value(10))
      ).eval()
      ClassDef("someClassName1") Extends ClassDef("someClassName")

      DeclareInstance(ClassDef("someClassName1"),"inst1").eval()
      AssignInstance(NewObject(ClassDef("someClassName1")), "inst1").eval()

      assert(Instance("inst1", GetField("a")).eval() == 47)
    }

    Scenario("classes must implement all methods of interface"){
      InterfaceDef("someInterface1",
        Field("a", AccessType.PUBLIC, Value(20)),
        Method("method1",
          AccessType.PUBLIC,
          ImplementationType.ABSTRACT,
          List()
        )
      ).eval()
      ClassDef("someClassName1",
        Field("x", AccessType.PRIVATE, Value(47)),
        Constructor(Value(1))
      ).eval()
      assertThrows[java.lang.Exception]{
        ClassDef("someClassName1") Implements List(InterfaceDef("someInterface1"))
      }
    }


    Scenario("Interface dynamic dispatch"){
      ClassDef("someClassName1",
        Field("x", AccessType.PRIVATE, Value(47)),
        Constructor(Value(1)),
        Method(
          "method1",
          AccessType.PUBLIC,
          ImplementationType.CONCRETE,
          List(),
          Value(1)
        )
      ).eval()
      ClassDef("someClassName1") Implements List(InterfaceDef("someInterface1"))
      DeclareInstance(InterfaceDef("someInterface1"),"inst1").eval()
      AssignInstance(NewObject(ClassDef("someClassName1")), "inst1").eval()
      assert(Instance("inst1", GetField("a")).eval() == 20)


    }
  }


  Feature("Abstract class creation"){

    Scenario("abstract class must have atleast one abstract method(s)"){

      assertThrows[java.lang.Exception]{
        AbstractClassDef("AbsClass1",
          Field("x", AccessType.PUBLIC, Value(10)),
        ).eval()
      }
      assertThrows[java.lang.Exception]{
        AbstractClassDef("AbsClass1",
          Field("x", AccessType.PUBLIC, Value(10)),
          Method("someMeth1",
            AccessType.PUBLIC,
            ImplementationType.CONCRETE,
            List()
          )
        ).eval()
      }
    }

    Scenario("classes must implement all abstract methods in inherited abstract class "){
      AbstractClassDef("AbsClass1",
        Field("y", AccessType.PUBLIC, Value(10)),
        Method("someMeth1",
          AccessType.PUBLIC,
          ImplementationType.ABSTRACT,
          List()
        )
      ).eval()
      ClassDef("someClassName1",
        Field("x", AccessType.PRIVATE, Value(47)),
        Constructor(Value(1))
      ).eval()
      assertThrows[java.lang.Exception]{
        ClassDef("someClassName1") Extends AbstractClassDef("AbsClass1")
      }

    }

    Scenario("class inheriting abstract class"){
      AbstractClassDef("AbsClass1",
        Field("y", AccessType.PUBLIC, Value(10)),
        Method("someMeth1",
          AccessType.PUBLIC,
          ImplementationType.ABSTRACT,
          List()
        )
      ).eval()
      ClassDef("someClassName1",
        Field("x", AccessType.PRIVATE, Value(47)),
        Constructor(Value(1)),
        Method("someMeth1",
          AccessType.PUBLIC,
          ImplementationType.CONCRETE,
          List(),
          Var("x")
        )
      ).eval()
      ClassDef("someClassName1") Extends AbstractClassDef("AbsClass1")
      DeclareInstance(ClassDef("someClassName1"), "inst2").eval()
      AssignInstance(NewObject(ClassDef("someClassName1")), "inst2").eval()
      assert(Instance("inst2", GetField("y")).eval() == 10)
    }

    Scenario("abstract class upcasting"){
      DeclareInstance(ClassDef("AbsClass1"), "inst3").eval()
      AssignInstance(NewObject(ClassDef("someClassName1")), "inst3").eval()
      assert(Instance("inst3", InvokeMethod("someMeth1", List())).eval() == 47)
    }


  }






}
