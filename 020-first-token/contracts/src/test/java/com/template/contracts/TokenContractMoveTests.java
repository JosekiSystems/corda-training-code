package com.template.contracts;

import kotlin.NotImplementedError;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.contracts.DummyContract;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import java.util.Arrays;

import static com.template.contracts.TestHelpers.createToken;
import static com.template.contracts.TokenContract.TOKEN_CONTRACT_ID;
import static net.corda.testing.node.NodeTestUtils.transaction;
import static org.junit.Assert.assertEquals;

public class TokenContractMoveTests {
    private final MockServices ledgerServices = new MockServices();
    private final TestIdentity alice = new TestIdentity(new CordaX500Name("Alice", "London", "GB"));
    private final TestIdentity bob = new TestIdentity(new CordaX500Name("Bob", "New York", "US"));
    private final TestIdentity carly = new TestIdentity(new CordaX500Name("Carly", "New York", "US"));

    @Test
    public void transactionMustIncludeATokenContractCommand() {
        transaction(ledgerServices, tx -> {
            tx.input(TOKEN_CONTRACT_ID, createToken(alice, bob, 10L));
            tx.output(TOKEN_CONTRACT_ID, createToken(alice, bob, 10L));
            tx.command(alice.getPublicKey(), new DummyContract.Commands.Create());
            tx.failsWith("Required com.template.contracts.TokenContract.Commands command");
            tx.command(bob.getPublicKey(), new TokenContract.Commands.Move());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void moveTransactionMustHaveInputs() {
        transaction(ledgerServices, tx -> {
            tx.output(TOKEN_CONTRACT_ID, createToken(alice, carly, 10L));
            tx.command(alice.getPublicKey(), new TokenContract.Commands.Move());
            tx.failsWith("There should be tokens to move.");
            return null;
        });
    }

    @Test
    public void moveTransactionMustHaveOutputs() {
        transaction(ledgerServices, tx -> {
            tx.input(TOKEN_CONTRACT_ID, createToken(alice, bob, 10L));
            tx.command(bob.getPublicKey(), new TokenContract.Commands.Move());
            tx.failsWith("There should be moved tokens.");
            return null;
        });
    }

    @Test
    // Testing this may be redundant as these wrong states would have to be issued first, but the contract would not
    // let that happen.
    public void inputsMustNotHaveAZeroQuantity() {
        transaction(ledgerServices, tx -> {
            tx.input(TOKEN_CONTRACT_ID, createToken(alice, bob, 10L));
            tx.input(TOKEN_CONTRACT_ID, createToken(alice, bob, 0L));
            tx.output(TOKEN_CONTRACT_ID, createToken(alice, bob, 10L));
            tx.command(bob.getPublicKey(), new TokenContract.Commands.Move());
            tx.failsWith("All quantities must be above 0.");
            return null;
        });
    }

    @Test
    // Testing this may be redundant as these wrong states would have to be issued first, but the contract would not
    // let that happen.
    public void inputsMustNotHaveNegativeQuantity() {
        transaction(ledgerServices, tx -> {
            tx.input(TOKEN_CONTRACT_ID, createToken(alice, bob, 10L));
            tx.input(TOKEN_CONTRACT_ID, createToken(alice, bob, -1L));
            tx.output(TOKEN_CONTRACT_ID, createToken(alice, bob, 9L));
            tx.command(bob.getPublicKey(), new TokenContract.Commands.Move());
            tx.failsWith("All quantities must be above 0.");
            return null;
        });
    }

    @Test
    public void outputsMustNotHaveAZeroQuantity() {
        transaction(ledgerServices, tx -> {
            tx.input(TOKEN_CONTRACT_ID, createToken(alice, bob, 10L));
            tx.output(TOKEN_CONTRACT_ID, createToken(alice, bob, 10L));
            tx.output(TOKEN_CONTRACT_ID, createToken(alice, carly, 0L));
            tx.command(bob.getPublicKey(), new TokenContract.Commands.Move());
            tx.failsWith("All quantities must be above 0.");
            return null;
        });
    }

    @Test
    public void outputsMustNotHaveNegativeQuantity() {
        transaction(ledgerServices, tx -> {
            tx.input(TOKEN_CONTRACT_ID, createToken(alice, bob, 10L));
            tx.output(TOKEN_CONTRACT_ID, createToken(alice, bob, 11L));
            tx.output(TOKEN_CONTRACT_ID, createToken(alice, carly, -1L));
            tx.command(bob.getPublicKey(), new TokenContract.Commands.Move());
            tx.failsWith("All quantities must be above 0.");
            return null;
        });
    }

    @Test
    public void issuerMustBeConservedInMoveTransaction() {
        transaction(ledgerServices, tx -> {
            tx.input(TOKEN_CONTRACT_ID, createToken(alice, bob, 10L));
            tx.output(TOKEN_CONTRACT_ID, createToken(carly, bob, 10L));
            tx.command(bob.getPublicKey(), new TokenContract.Commands.Move());
            tx.failsWith("Consumed and created issuers should be identical.");
            return null;
        });
    }

    @Test
    public void allIssuersMustBeConservedInMoveTransaction() {
        transaction(ledgerServices, tx -> {
            tx.input(TOKEN_CONTRACT_ID, createToken(alice, bob, 10L));
            tx.input(TOKEN_CONTRACT_ID, createToken(carly, bob, 10L));
            tx.output(TOKEN_CONTRACT_ID, createToken(alice, bob, 20L));
            tx.command(bob.getPublicKey(), new TokenContract.Commands.Move());
            tx.failsWith("Consumed and created issuers should be identical.");
            return null;
        });
    }

    @Test
    public void sumMustBeConservedInMoveTransaction() {
        transaction(ledgerServices, tx -> {
            tx.input(TOKEN_CONTRACT_ID, createToken(alice, bob, 10L));
            tx.input(TOKEN_CONTRACT_ID, createToken(alice, bob, 15L));
            tx.output(TOKEN_CONTRACT_ID, createToken(alice, bob, 20L));
            tx.command(bob.getPublicKey(), new TokenContract.Commands.Move());
            tx.failsWith("The sum of quantities for each issuer should be conserved.");
            return null;
        });
    }

    @Test
    public void allSumsPerIssuerMustBeConservedInMoveTransaction() {
        transaction(ledgerServices, tx -> {
            tx.input(TOKEN_CONTRACT_ID, createToken(alice, bob, 10L));
            tx.input(TOKEN_CONTRACT_ID, createToken(alice, bob, 15L));
            tx.output(TOKEN_CONTRACT_ID, createToken(alice, bob, 20L));
            tx.input(TOKEN_CONTRACT_ID, createToken(carly, bob, 10L));
            tx.input(TOKEN_CONTRACT_ID, createToken(carly, bob, 15L));
            tx.output(TOKEN_CONTRACT_ID, createToken(carly, bob, 30L));
            tx.command(bob.getPublicKey(), new TokenContract.Commands.Move());
            tx.failsWith("The sum of quantities for each issuer should be conserved.");
            return null;
        });
    }

    @Test
    public void sumsThatResultInOverflowAreNotPossibleInMoveTransaction() {
        try {
            transaction(ledgerServices, tx -> {
                tx.input(TOKEN_CONTRACT_ID, createToken(alice, bob, Long.MAX_VALUE));
                tx.input(TOKEN_CONTRACT_ID, createToken(alice, carly, 1L));
                tx.output(TOKEN_CONTRACT_ID, createToken(alice, bob, 1L));
                tx.output(TOKEN_CONTRACT_ID, createToken(alice, carly, Long.MAX_VALUE));
                tx.command(Arrays.asList(bob.getPublicKey(), carly.getPublicKey()), new TokenContract.Commands.Move());
                tx.failsWith("The sum of quantities for each issuer should be conserved.");
                return null;
            });
            throw new NotImplementedError("Should not reach here");
        } catch (AssertionError e) {
            assertEquals(ArithmeticException.class, e.getCause().getCause().getClass());
        }
    }

    @Test
    public void currentHolderMustSignMoveTransaction() {
        transaction(ledgerServices, tx -> {
            tx.input(TOKEN_CONTRACT_ID, createToken(alice, bob, 10L));
            tx.output(TOKEN_CONTRACT_ID, createToken(alice, carly, 10L));
            tx.command(alice.getPublicKey(), new TokenContract.Commands.Move());
            tx.failsWith("The current holders should sign.");
            return null;
        });
    }

    @Test
    public void allCurrentHoldersMustSignMoveTransaction() {
        transaction(ledgerServices, tx -> {
            tx.input(TOKEN_CONTRACT_ID, createToken(alice, bob, 10L));
            tx.input(TOKEN_CONTRACT_ID, createToken(alice, carly, 20L));
            tx.output(TOKEN_CONTRACT_ID, createToken(alice, carly, 30L));
            tx.command(bob.getPublicKey(), new TokenContract.Commands.Move());
            tx.failsWith("The current holders should sign.");
            return null;
        });
    }

    @Test
    public void canHaveDifferentIssuersInMoveTransaction() {
        transaction(ledgerServices, tx -> {
            tx.input(TOKEN_CONTRACT_ID, createToken(alice, bob, 10L));
            tx.input(TOKEN_CONTRACT_ID, createToken(alice, bob, 20L));
            tx.output(TOKEN_CONTRACT_ID, createToken(alice, alice, 5L));
            tx.output(TOKEN_CONTRACT_ID, createToken(alice, bob, 5L));
            tx.output(TOKEN_CONTRACT_ID, createToken(alice, carly, 20L));
            tx.input(TOKEN_CONTRACT_ID, createToken(carly, carly, 40L));
            tx.output(TOKEN_CONTRACT_ID, createToken(carly, alice, 20L));
            tx.output(TOKEN_CONTRACT_ID, createToken(carly, bob, 20L));
            tx.command(Arrays.asList(bob.getPublicKey(), carly.getPublicKey()), new TokenContract.Commands.Move());
            tx.verifies();
            return null;
        });
    }

}