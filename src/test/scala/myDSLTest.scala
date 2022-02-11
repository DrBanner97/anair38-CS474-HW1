
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec
import myDSL.myDSL.Exp.*

import java.util.NoSuchElementException


class myDSLTest extends AnyFeatureSpec with GivenWhenThen {

  Feature("Set Creation and modification"){

    Scenario("Creating and Assigning a set"){
      DeclareVar("a", Insert(CreateSet(Value(1),Value(2),Value(3)),Value(4))).eval()
      assert(Var("a").eval() == Set(1,2,3,4))
    }

    Scenario("Inserting into set"){
      Assign("a", Insert(Var("a"), Value(5))).eval()
      assert(Var("a").eval() == Set(1,2,3,4,5))
    }

    Scenario("Deleting from set"){
      Assign("a", Delete(Var("a"), Value(4), Value(5))).eval()
      assert(Var("a").eval() == Set(1,2,3))
    }

  }

  Feature("Binary Set Operations"){
    Scenario("Union operation"){
      DeclareVar("b", CreateSet(Value(3), Value(4), Value(5))).eval()
      assert(Union(Var("a"), Var("b")).eval() == Set(1,2,3,4,5))
    }

    Scenario("Diff operation"){
      assert(Diff(Var("a"), Var("b")).eval() == Set(1,2))
      assert(Diff(Var("b"), Var("a")).eval() == Set(4,5))
    }

    Scenario("Intersection operation"){
      assert(Intersection(Var("a"), Var("b")).eval() == Set(3))
    }

    Scenario("Symmetric Difference operation"){
      assert(SymmetricDiff(Var("a"), Var("b")).eval() == Set(1,2,4,5))
    }
    Scenario("Cartesian Product operation"){
      assert(Product(Var("a"), Var("b")).eval() == Set((3,4), (2,5), (1,4), (2,4), (1,5), (3,3), (1,3), (2,3), (3,5)))
    }
  }



  Feature("Scoping"){
    Scenario("Defining Inner scopes"){
      Scope("s1", DeclareVar("c", Insert(CreateSet(Value(6), Value(7), Value(8)), Value(9), Value(10)))).eval()
      assert(Scope("s1", Var("c")).eval() == Set(6,7,8,9,10))
    }


    Scenario("accessing variables defined in Scope from outside "){
      //globally accessing variable defined in a scope
      assertThrows[NoSuchElementException]{
        Var("c").eval()
      }
      //Wrong scope name
      assertThrows[NoSuchElementException]{
        Scope("s2", Var("c")).eval()
      }
    }

    Scenario("accessing variables defined outside of scope"){
      assert(Scope("s1",Var("a")).eval() == Set(1,2,3))
    }

    Scenario("Overshadowing variables"){
      Scope("s1", DeclareVar("a", Value(42))).eval()
      assert(Scope("s1",Var("a")).eval() == 42)
      assert(Var("a").eval() == Set(1,2,3))
    }

    Scenario("Anonymous Scope"){
      AnonScope(DeclareVar("d", CreateSet(Value(109)))).eval()
      assertThrows[NoSuchElementException]{
        Var("d").eval()
      }
      AnonScope(Assign("a", Value(77))).eval()
      assert(Var("a").eval() == 77)
    }

  }

  Feature("Macros"){

    Scenario("Creating and Evaluating Macro"){

      Assign("a", CreateSet(Value(1),Value(2),Value(3))).eval()

      SetMacro("combine_sets", Scope("s1",Union(Var("c"),Var("b")))).eval()
      assert(Union(Var("a"), GetMacro("combine_sets")).eval() == Set(1,2,3,4,5,6,7,8,9,10))
    }

  }



}
