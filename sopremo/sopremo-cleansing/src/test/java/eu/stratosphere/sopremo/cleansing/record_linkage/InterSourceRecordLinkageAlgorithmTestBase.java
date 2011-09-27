package eu.stratosphere.sopremo.cleansing.record_linkage;

import static eu.stratosphere.sopremo.JsonUtil.createObjectNode;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import eu.stratosphere.pact.common.type.KeyValuePair;
import eu.stratosphere.sopremo.EvaluationContext;
import eu.stratosphere.sopremo.expressions.ConstantExpression;
import eu.stratosphere.sopremo.expressions.EvaluationExpression;
import eu.stratosphere.sopremo.expressions.ObjectAccess;
import eu.stratosphere.sopremo.expressions.ObjectCreation;
import eu.stratosphere.sopremo.jsondatamodel.ArrayNode;
import eu.stratosphere.sopremo.jsondatamodel.JsonNode;
import eu.stratosphere.sopremo.testing.SopremoTestPlan;
import eu.stratosphere.sopremo.testing.SopremoTestPlan.Input;

/**
 * Base for inner source {@link InterSourceRecordLinkage} test cases between at least two sources.
 * 
 * @author Arvid Heise
 * @param <P>
 *        the {@link RecordLinkageAlgorithm}
 */
@RunWith(Parameterized.class)
@Ignore
public abstract class InterSourceRecordLinkageAlgorithmTestBase<P extends RecordLinkageAlgorithm> extends
		RecordLinkageAlgorithmTestBase {
	private final EvaluationExpression resultProjection1, resultProjection2;

	private SopremoTestPlan sopremoTestPlan;

	/**
	 * Initializes InterSourceRecordLinkageAlgorithmTestBase.
	 * 
	 * @param resultProjection1
	 * @param resultProjection2
	 */
	protected InterSourceRecordLinkageAlgorithmTestBase(EvaluationExpression resultProjection1,
			EvaluationExpression resultProjection2) {
		this.resultProjection1 = resultProjection1;
		this.resultProjection2 = resultProjection2;
	}

	/**
	 * Performs the naive record linkage in place and compares with the Pact code.
	 */
	@Test
	public void pactCodeShouldPerformLikeStandardImplementation() {
		final InterSourceRecordLinkage recordLinkage = new InterSourceRecordLinkage(this.createAlgorithm(), new ConstantExpression(1), 0,
			null,
			null);
		this.sopremoTestPlan = createTestPlan(recordLinkage, false, this.resultProjection1, this.resultProjection2);

		this.generateExpectedPairs(this.sopremoTestPlan.getInput(0), this.sopremoTestPlan.getInput(1));

		try {
			this.sopremoTestPlan.run();
		} catch (final AssertionError error) {
			throw new AssertionError(String.format("For test %s: %s", this, error.getMessage()));
		}
	}

	/**
	 * Generates the expected pairs and invokes {@link #emitCandidate(KeyValuePair, KeyValuePair)}.
	 * 
	 * @param leftInput
	 * @param rightInput
	 */
	protected abstract void generateExpectedPairs(Input leftInput, Input rightInput);

	/**
	 * Emit the candidate.
	 * 
	 * @param left
	 * @param right
	 */
	protected void emitCandidate(KeyValuePair<JsonNode, JsonNode> left, KeyValuePair<JsonNode, JsonNode> right) {
		EvaluationExpression resultProjection1 = this.resultProjection1, resultProjection2 = this.resultProjection2;
		if (resultProjection1 == null)
			resultProjection1 = EvaluationExpression.VALUE;
		if (resultProjection2 == null)
			resultProjection2 = EvaluationExpression.VALUE;

		final EvaluationContext context = this.getContext();

		this.sopremoTestPlan.getExpectedOutput(0).add(
			new ArrayNode(resultProjection1.evaluate(left.getValue(), context),
				resultProjection2.evaluate(right.getValue(), context)));
	}

	@Override
	public String toString() {
		return String.format("resultProjection1=%s, resultProjection2=%s", this.resultProjection1,
			this.resultProjection2);
	}

	/**
	 * Returns the context of the test plan.
	 * 
	 * @return the context
	 */
	protected EvaluationContext getContext() {
		return this.sopremoTestPlan.getEvaluationContext();
	}

	/**
	 * Creates the algorithm with the similarityFunction and threshold
	 * 
	 * @return the configured algorithm
	 */
	protected abstract RecordLinkageAlgorithm createAlgorithm();

	/**
	 * Creates a test plan for the record linkage operator.
	 * 
	 * @param recordLinkage
	 * @param useId
	 * @param resultProjection1
	 * @param resultProjection2
	 * @return the generated test plan
	 */
	protected static SopremoTestPlan createTestPlan(final InterSourceRecordLinkage recordLinkage, final boolean useId,
			final EvaluationExpression resultProjection1, final EvaluationExpression resultProjection2) {
		final SopremoTestPlan sopremoTestPlan = new SopremoTestPlan(recordLinkage);
		if (useId) {
			recordLinkage.getRecordLinkageInput(0).setIdProjection(new ObjectAccess("id"));
			recordLinkage.getRecordLinkageInput(1).setIdProjection(new ObjectAccess("id2"));
		}
		if (resultProjection1 != null)
			recordLinkage.getRecordLinkageInput(0).setResultProjection(resultProjection1);
		if (resultProjection2 != null)
			recordLinkage.getRecordLinkageInput(1).setResultProjection(resultProjection2);
		Object[] fields = { "id", 0, "first name", "albert", "last name", "perfect duplicate", "age", 80 };
		Object[] fields1 = { "id", 1, "first name", "berta", "last name", "typo", "age", 70 };
		Object[] fields2 = { "id", 2, "first name", "charles", "last name", "age inaccurate", "age", 70 };
		Object[] fields3 = { "id", 3, "first name", "dagmar", "last name", "unmatched", "age", 75 };
		Object[] fields4 = { "id", 4, "first name", "elma", "last name", "firstNameDiffers", "age", 60 };

		sopremoTestPlan.getInput(0).
			add((JsonNode) createObjectNode(fields)).
			add((JsonNode) createObjectNode(fields1)).
			add((JsonNode) createObjectNode(fields2)).
			add((JsonNode) createObjectNode(fields3)).
			add((JsonNode) createObjectNode(fields4));
		Object[] fields5 = { "id2", 10, "firstName", "albert", "lastName", "perfect duplicate", "age", 80 };
		Object[] fields6 = { "id2", 11, "firstName", "berta", "lastName", "tpyo", "age", 70 };
		Object[] fields7 = { "id2", 12, "firstName", "charles", "lastName", "age inaccurate", "age", 69 };
		Object[] fields8 = { "id2", 14, "firstName", "elmar", "lastName", "firstNameDiffers", "age", 60 };
		sopremoTestPlan.getInput(1).
			add((JsonNode) createObjectNode(fields5)).
			add((JsonNode) createObjectNode(fields6)).
			add((JsonNode) createObjectNode(fields7)).
			add((JsonNode) createObjectNode(fields8));
		return sopremoTestPlan;
	}

	/**
	 * Returns a duplicate projection expression that collects some fields.
	 * 
	 * @return an aggregating expression
	 */
	protected static EvaluationExpression getAggregativeProjection1() {
		final ObjectCreation aggregating = new ObjectCreation();
		aggregating.addMapping("name", new ObjectAccess("first name"));
		aggregating.addMapping("id", new ObjectAccess("id"));

		return aggregating;
	}

	/**
	 * Returns a duplicate projection expression that collects some fields.
	 * 
	 * @return an aggregating expression
	 */
	protected static EvaluationExpression getAggregativeProjection2() {
		final ObjectCreation aggregating = new ObjectCreation();
		aggregating.addMapping("name", new ObjectAccess("firstName"));
		aggregating.addMapping("id", new ObjectAccess("id2"));

		return aggregating;
	}

}
