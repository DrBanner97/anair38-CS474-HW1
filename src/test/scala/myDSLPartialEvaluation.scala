
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec
import myDSL.myDSL.Exp.*
import myDSL.myDSL.*
import scala.collection.immutable.ArraySeq


class myDSLPartialEvaluation extends AnyFeatureSpec with GivenWhenThen {


  Feature("Partial Evaluation"){


    Scenario("complete IF code structure"){

      DeclareVar("a", CreateSet(Value(1), Value(2), Value(3))).eval()
      DeclareVar("y", Value(2)).eval()
      assert(
        If(
        Check(Var("a"), Var("b")),
        AnonScope(Var("x"), Var("y"), Value(3)),
        AnonScope(Insert(Var("a"), Value(4))),
      ).eval() ==
          Left(
            If(Check(Value(Set(1, 2, 3)),Var("b")),
              AnonScope(ArraySeq[Exp](Var("x"), Value(2), Value(3)):_*),
              AnonScope(ArraySeq[Exp](Value(Set(1, 2, 3, 4))):_*))
          )
      )
    }


    Scenario("Complete evaluation"){


      DeclareVar("b", CreateSet(Value(2), Value(3), Value(4))).eval()

      assert(
        If(
          Check(Var("a"), Var("b")),
          AnonScope(Var("x"), Var("y"), Value(3)),
          AnonScope(Insert(Var("a"), Value(4))),
        ).eval() ==

          Right(
            ArraySeq(Set(1, 2, 3, 4))
          )


      )
    }

    Scenario("Monad Test 1"){

      def CartProductComputation(exp: Exp):Exp = {
        exp.eval() match{

          case Right(immutableSet: Set[Any])=>
            Value(Product(Value(immutableSet), Value(immutableSet)).eval().merge)
          case Left(value: Exp)=>
            Product(value,value)
        }
      }

      assert(myDSLExpMonad(CreateSet(Value(1), Value(2), Value(3)))
        .map(CartProductComputation) == Value(Set((2,2), (2,1), (1,2), (1,1), (3,2), (3,1), (3,3), (2,3), (1,3))))


    }

    Scenario("Partially evaluated Monad"){
      def CartProductComputation(exp: Exp):Exp = {
        exp.eval() match{

          case Right(immutableSet: Set[Any])=>
            Value(Product(Value(immutableSet), Value(immutableSet)).eval().merge)
          case Left(value: Exp)=>
            Product(value,value)
        }
      }
      assert(myDSLExpMonad(Var("x")).map(CartProductComputation) == Product(Var("x"), Var("x")))

    }


    Scenario("chained Monads"){
      def CartProductComputation(exp: Exp):Exp = {
        exp.eval() match{

          case Right(immutableSet: Set[Any])=>
            Value(Product(Value(immutableSet), Value(immutableSet)).eval().merge)
          case Left(value: Exp)=>
            Product(value,value)
        }
      }


      assert(myDSLExpMonad(myDSLExpMonad(CreateSet(Value(1), Value(2), Value(3)))
        .map(CartProductComputation)).map(e=>Value( Diff(e,e).eval().merge)) == Value(Set()))
    }

  }



}
