package com.al333z.antitest.kernel

import cats.{Comonad, MonadError}
import org.scalatest.FeatureSpecLike

import scala.collection.immutable
import scala.language.higherKinds
import scala.util.Try

trait FeatureRunner[F[_]] extends FeatureSpecLike {

  def runFeature[FeatureDeps](f: Feature[F, FeatureDeps])
                             (implicit me: MonadError[F, Vector[String]], cm: Comonad[F]): Unit = feature(f.description) {
    val featureDeps: FeatureDeps = f.beforeAll()
    val scenarios = f.scenarios
    val indexes: immutable.Seq[Int] = scenarios.indices

    scenarios.zip(indexes).foreach {
      case (s, i) =>
        val description = i + ". " + s.description + {
          s match {
            case SampleScenario(_, _, _, _, sample) ⇒ " " + sample
            case _ ⇒ ""
          }
        }
        scenario(description) {
          val scenarioDeps: s.ScenarioDeps = s.before(featureDeps)
          val result = Try(cm.extract(s.behaviour(featureDeps, scenarioDeps).run)).toEither

          try {
            s.after(scenarioDeps)
            if(i == (scenarios.length -1))
              f.afterAll(featureDeps) // FIXME we can do better than this
          }
          finally {
            verify(result)
          }
        }
    }
  }

  private def verify(testResult: Either[Throwable, (Vector[String], Unit)]) = {
    testResult match {
      case Right((steps, _)) => {
        println(steps.mkString("\n") + "\nPASSED\n")
        assert(true)
      }
      case Left(t) => {
        fail(t.getMessage + "\nFAILED\n", t)
      }
    }
  }
}
