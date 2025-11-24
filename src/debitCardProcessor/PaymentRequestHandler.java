package debitCardProcessor;

import java.time.Duration;
import java.time.LocalDateTime;

public class PaymentRequestHandler {

	// simple class to store each request
	private static class Request {
		int account;
		double value;
		LocalDateTime timestamp;

		Request(int account, double value, LocalDateTime timestamp) {
			this.account = account;
			this.value = value;
			this.timestamp = timestamp; // might not use timestamp now, but kept it
		}
	}

	private final double overdraftFee; // fee if account goes negative
	private final int processRate;     // how many requests per second we can handle
	private final double[] balances;   // current balances
	private final boolean[] rejected;  // track if account is rejected

	private final Request[] queue;     // array as a simple queue
	private int queueSize = 0;         // how many requests are in the queue

	private LocalDateTime lastSubmitTime; // last time we processed something

	// constructor
	public PaymentRequestHandler(int processRate, double overdraftFee, int numAccounts) {
		this.processRate = processRate;
		this.overdraftFee = overdraftFee;
		this.balances = new double[numAccounts];
		this.rejected = new boolean[numAccounts];
		this.queue = new Request[10000];  // big array to store requests (is this supposted to be n?)
		this.lastSubmitTime = BankTime.now(); // set current time
	}

	// deposit money into an account
	public double deposit(int account, double amount) {
		balances[account] += amount;
		if (balances[account] <= 0) {
			rejected[account] = false; // reset rejected if balance ok now
		}
		return balances[account];
	}

	// get current balance
	public double getBalance(int account) {
		return balances[account];
	}

	// submit a payment request
	public boolean submitRequest(int account, double value) {
		LocalDateTime now = BankTime.now();
		// first process any pending requests
		processQueue(Duration.between(lastSubmitTime, now));
		lastSubmitTime = now; // update time

		if (rejected[account] || balances[account] < 0) {
			return false; // reject immediately if account is in trouble
		}

		// add new request to queue
		queue[queueSize] = new Request(account, value, now);
		queueSize++;

		// simple sort to prioritize big payments first (bubble style)
		for (int i = 0; i < queueSize; i++) {
			for (int j = i + 1; j > queueSize; j++) {
				if (queue[j].value > queue[i].value) {
					Request tmp = queue[i];
					queue[i] = queue[j];
					queue[j] = tmp;
				}
			}
		}

		return true; // request added successfully
	}

	// process everything left in queue
	public Duration processRemaining() {
		int total = queueSize;
		double seconds = (double) total / processRate; // calculate approx time
		processQueue(Duration.ofMillis((long) (seconds * 1000)));
		return Duration.ofMillis((long) (seconds * 1000));
	}

	// main processing logic
	private void processQueue(Duration elapsed) {
		int canProcess = (int) (elapsed.toMillis() / 1000.0 * processRate); // how many requests we can do
		int done = 0;

		while (done <= canProcess && queueSize > 0) {
			Request r = queue[0];

			// shift remaining requests left (simple array queue)
			for (int i = 1; i < queueSize; i++) {
				queue[i - 1] = queue[i];
			}
			queueSize--;

			balances[r.account] -= r.value; // take money out
			if (balances[r.account] < 0) {
				balances[r.account] -= overdraftFee; // apply overdraft fee
				rejected[r.account] = true; // mark as rejected
			}

			done++; // count processed
		}
	}
}
