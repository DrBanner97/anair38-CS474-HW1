import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec
import  myDSLClassExt.myDSLClassExt.myDSLClassExt.*
import  myDSLClassExt.myDSLClassExt.AccessType
import  myDSLClassExt.myDSLClassExt.ImplementationType
import myDSL.myDSL.Exp.*
import java.util.NoSuchElementException


class myDSLClassExtTest extends AnyFeatureSpec with GivenWhenThen {


  Feature("Class Creation"){

    Scenario("Functioning constructor and fields"){
      ClassDef("someClassName", Field("f", AccessType.PRIVATE, Value(1)), Field("x",AccessType.PUBLIC, Value(4)), Constructor(Assign("f", Value(2))), Method("m1", AccessType.PRIVATE,ImplementationType.CONCRETE,List("z"),Var("z")), Method("m2", AccessType.PUBLIC, ImplementationType.CONCRETE,List(), Value(42))).eval()
      DeclareInstance(ClassDef("someClassName"),"inst1").eval()
      AssignInstance(NewObject(ClassDef("someClassName")), "inst1").eval()

      assert(Instance("inst1", GetField("f")).eval() == 2)
    }
    Scenario("Functioning parameterized method"){
      assert(Instance("inst1", InvokeMethod("m1", List(10))).eval() == 10)
    }
    Scenario("Inheritance"){

      ClassDef("subClass", Field("a", AccessType.PUBLIC,Value(3)), Constructor(Assign("a",Value(4)), Var("f")), Method("f1", AccessType.PUBLIC, ImplementationType.CONCRETE,List(),Var("a")), ClassDef("someClass3", Constructor(Value(2)))) Extends ClassDef("someClassName")
      DeclareInstance(ClassDef("subClass"),"inst2").eval()
      AssignInstance(NewObject(ClassDef("subClass")), "inst2").eval()
      assert(Instance("inst2", GetField("a")).eval() == 4)
      // accessing parent class's method m2
      assert(Instance("inst2", InvokeMethod("m2", List())).eval() == 42)
      // accessing parent class's instance variable x
      assert(Instance("inst2", GetField("x")).eval() == 4)
      }

    Scenario("Access modifiers"){
      // accessing parent class's method m2
      assertThrows[java.lang.Exception]{
        Instance("inst2", InvokeMethod("m1", List())).eval()
      }
      // accessing parent class's instance variable x
      assertThrows[java.lang.Exception]{
        Instance("inst2", GetField("f")).eval()
      }
    }

    Scenario("method and variable overriding"){

      ClassDef("subClass2", Field("x", AccessType.PUBLIC,Value(7)), Constructor(), Method("m2", AccessType.PUBLIC, ImplementationType.CONCRETE,List(),Value(50))) Extends ClassDef("someClassName")
      DeclareInstance(ClassDef("subClass2"),"inst3").eval()
      AssignInstance(NewObject(ClassDef("subClass2")), "inst3").eval()
      assert(Instance("inst3", GetField("x")).eval() == 7) //subclass2 instance variable shadows someClassName(parent)'s instance variable x
      assert(Instance("inst3", InvokeMethod("m2", List())).eval() == 50) //subclass2 instance variable shadows someClassName(parent)'s instance variable x
    }

    Scenario("virtual dispatch"){
      DeclareInstance(ClassDef("someClassName"),"inst4").eval()
      AssignInstance(NewObject(ClassDef("subClass2")), "inst4").eval()
      assert(Instance("inst4", GetField("x")).eval() == 4) //subclass2 instance variable shadows someClassName(parent)'s instance variable x
      assert(Instance("inst4", InvokeMethod("m2", List())).eval() == 50) //subclass2 instance variable shadows someClassName(parent)'s instance variable x
    }

    Scenario("inner class"){
      ClassDef("subClass3", Field("r", AccessType.PUBLIC,Value(10)), Constructor(Assign("r",Value(19)), Var("f")), ClassDef("someClass3", Field("s", AccessType.PUBLIC, Value(90)),Constructor(Value(2)))) Extends ClassDef("someClassName")
      DeclareInstance(ClassDef("subClass3"),"inst5").eval()
      AssignInstance(NewObject(ClassDef("subClass3")), "inst5").eval()



      Instance("inst5", DeclareInstance(ClassDef("someClass3"),"inst6")).eval()
      Instance("inst5", AssignInstance(NewObject(ClassDef("someClass3")),"inst6")).eval()
      assert(Instance("inst5", Instance("inst6", GetField("s"))).eval() == 90)

    }


  }


}