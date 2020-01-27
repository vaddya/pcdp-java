package edu.coursera.concurrent;

import edu.rice.pcdp.Actor;

import static edu.rice.pcdp.PCDP.finish;

/**
 * An actor-based implementation of the Sieve of Eratosthenes.
 * <p>
 * TODO Fill in the empty SieveActorActor actor class below and use it from
 * countPrimes to determine the number of primes <= limit.
 */
public final class SieveActor extends Sieve {
    /**
     * {@inheritDoc}
     * <p>
     * TODO Use the SieveActorActor class to calculate the number of primes <=
     * limit in parallel. You might consider how you can model the Sieve of
     * Eratosthenes as a pipeline of actors, each corresponding to a single
     * prime number.
     */
    @Override
    public int countPrimes(final int limit) {
        final SieveActorActor actor = new SieveActorActor(2);
        finish(() -> {
            for (int i = 3; i <= limit; i += 2) {
                actor.send(i);
            }
            actor.send(0);
        });

        int count = 0;
        SieveActorActor it = actor;
        while (it != null) {
            count += it.getPrimesCount();
            it = it.getNext();
        }
        return count;
    }

    /**
     * An actor class that helps implement the Sieve of Eratosthenes in
     * parallel.
     */
    public static final class SieveActorActor extends Actor {
        private static final int MAX_PRIMES = 100;

        private final int[] primes;
        private int primesCount;
        private SieveActorActor next;

        SieveActorActor(final int prime) {
            this.primes = new int[MAX_PRIMES];
            this.primes[0] = prime;
            this.primesCount = 1;
            this.next = null;
        }

        int getPrimesCount() {
            return primesCount;
        }

        SieveActorActor getNext() {
            return next;
        }

        /**
         * Process a single message sent to this actor.
         * <p>
         * TODO complete this method.
         *
         * @param msg Received message
         */
        @Override
        public void process(final Object msg) {
            final int candidate = (Integer) msg;
            if (candidate <= 0) {
                if (next != null) {
                    next.send(msg);
                }
                return;
            }

            // is not local prime, ignore it
            if (!isLocalPrime(candidate)) {
                return;
            }

            // there is space in array
            if (primesCount < MAX_PRIMES) {
                primes[primesCount++] = candidate;
                return;
            }

            // there is no space in array, pass on the chain
            if (next == null) {
                next = new SieveActorActor(candidate);
            }
            next.send(msg);
        }

        private boolean isLocalPrime(final int candidate) {
            for (int i = 0; i < primesCount; i++) {
                if (candidate % primes[i] == 0) {
                    return false;
                }
            }
            return true;
        }
    }
}
