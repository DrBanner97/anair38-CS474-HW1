
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec

import myDSL.myDSL.Exp.*


class myDSLTest extends AnyFeatureSpec with GivenWhenThen {


  feature("Set Creation and modificaation"){

    Scenario("Creating and Assigning a set"){
      Assign("a", CreateSet(Value(1),Value(2),Value(3))).eval()
      assert(Var("a").eval() == Set(1,2,3))
    }

    Scenario("Inserting into set"){
      Assign("a", Insert(Var("a"), Value(4))).eval()
      assert(Var("a").eval() == Set(1,2,3,4))
    }

    Scenario("Deleting from set"){
      Assign("a", Delete(Var("a"), Value(4))).eval()
      assert(Var("a").eval() == Set(1,2,3))
    }

  }


  feature("Binary Set Operations"){
    Scenario("Union operation"){
      Assign("a", CreateSet(Value(1),Value(2),Value(3))).eval()
      Assign("b", CreateSet(Value(3), Value(4), Value(5))).eval()
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




}
