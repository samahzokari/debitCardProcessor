package test;

import debitCardProcessor.BankTime;
import debitCardProcessor.PaymentRequestHandler;
import static test.BkTest.assertEquals;
import static test.BkTest.assertFalse;
import static test.BkTest.runSuites;

/**
 * Run a handful of tests to verify basic functionality of your class.
 */
public class BankTests {
	private final PaymentRequestHandler handler = new PaymentRequestHandler(1, 35.00, 1);

	/** Advance the simulated clock by msLater milliseconds and then submit a payment request. */
	private static boolean submitRequest(PaymentRequestHandler handler, int msLater, int account, double value) {
		BankTime.advance(msLater, java.time.temporal.ChronoUnit.MILLIS);
		return handler.submitRequest(account, value);
	}

	/** Process all remaining requests and advance the simulated clock by the time required. */
	private static void processRemaining(PaymentRequestHandler handler) {
		BankTime.advance(handler.processRemaining());
	}

	@Test
	public void WithoutOverdraft() {
		handler.deposit(0, 100);
		submitRequest(handler, 1000, 0, 25);
		submitRequest(handler, 1000, 0, 25);
		submitRequest(handler, 1000, 0, 25);
		submitRequest(handler, 1000, 0, 25);
		processRemaining(handler);
		assertEquals(0, handler.getBalance(0));
	}

	@Test
	public void WithOneOverdraft() {
		handler.deposit(0, 75);
		submitRequest(handler, 1000, 0, 25);
		submitRequest(handler, 1000, 0, 25);
		submitRequest(handler, 1000, 0, 25);
		submitRequest(handler, 1000, 0, 75);
		processRemaining(handler);
		assertEquals(-110, handler.getBalance(0));
	}

	@Test
	public void WithMultipleOverdraft() {
		handler.deposit(0, 25);
		submitRequest(handler, 300, 0, 25);
		submitRequest(handler, 300, 0, 25);
		submitRequest(handler, 300, 0, 25);
		processRemaining(handler);
		assertEquals(-120, handler.getBalance(0));
	}

	@Test
	public void WithUnfairOverdraft() {
		handler.deposit(0, 75);
		submitRequest(handler, 250, 0, 25);
		submitRequest(handler, 250, 0, 25);
		submitRequest(handler, 250, 0, 25);
		submitRequest(handler, 250, 0, 75);
		processRemaining(handler);
		assertEquals(-180, handler.getBalance(0));
	}

	@Test
	public void WithRejectedPayment() {
		handler.deposit(0, 24);
		submitRequest(handler, 1000, 0, 25);
		assertFalse(submitRequest(handler, 1000, 0, 25));
		processRemaining(handler);
		assertEquals(-36.0, handler.getBalance(0));
	}

	/** Main method to run all tests */
	public static void main(String[] args) {
		runSuites("test.BankTests");
	}
}
