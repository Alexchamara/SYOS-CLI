package cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;

import cli.CliCommand;

import java.util.logging.Logger;

@DisplayName("CliCommand Abstract Base Class Tests")
class CliCommandTest {

    private TestCliCommand testCliCommand;

    @BeforeEach
    void setUp() {
        testCliCommand = new TestCliCommand();
    }

    @Nested
    @DisplayName("Abstract Method Implementation Tests")
    class AbstractMethodImplementationTests {

        @Test
        @DisplayName("Should require execute method implementation")
        void shouldRequireExecuteMethodImplementation() {
            // Given
            String[] args = {"arg1", "arg2"};

            // When
            testCliCommand.execute(args);

            // Then
            assertTrue(testCliCommand.executeWasCalled);
            assertArrayEquals(args, testCliCommand.receivedArgs);
        }

        @Test
        @DisplayName("Should require getName method implementation")
        void shouldRequireGetNameMethodImplementation() {
            // When
            String name = testCliCommand.getName();

            // Then
            assertEquals("test-command", name);
        }

        @Test
        @DisplayName("Should require getHelp method implementation")
        void shouldRequireGetHelpMethodImplementation() {
            // When
            String help = testCliCommand.getHelp();

            // Then
            assertEquals("Test command help text", help);
        }

        @Test
        @DisplayName("Should handle execute with empty arguments")
        void shouldHandleExecuteWithEmptyArguments() {
            // Given
            String[] emptyArgs = {};

            // When
            testCliCommand.execute(emptyArgs);

            // Then
            assertTrue(testCliCommand.executeWasCalled);
            assertEquals(0, testCliCommand.receivedArgs.length);
        }

        @Test
        @DisplayName("Should handle execute with null arguments")
        void shouldHandleExecuteWithNullArguments() {
            // Given
            String[] nullArgs = null;

            // When
            testCliCommand.execute(nullArgs);

            // Then
            assertTrue(testCliCommand.executeWasCalled);
            assertNull(testCliCommand.receivedArgs);
        }

        @Test
        @DisplayName("Should handle execute with multiple arguments")
        void shouldHandleExecuteWithMultipleArguments() {
            // Given
            String[] multipleArgs = {"command", "--option", "value", "--flag"};

            // When
            testCliCommand.execute(multipleArgs);

            // Then
            assertEquals(4, testCliCommand.receivedArgs.length);
            assertEquals("command", testCliCommand.receivedArgs[0]);
            assertEquals("--option", testCliCommand.receivedArgs[1]);
            assertEquals("value", testCliCommand.receivedArgs[2]);
            assertEquals("--flag", testCliCommand.receivedArgs[3]);
        }
    }

    @Nested
    @DisplayName("Logger Access Tests")
    class LoggerAccessTests {

        @Test
        @DisplayName("Should provide access to logger")
        void shouldProvideAccessToLogger() {
            // When
            Logger logger = testCliCommand.getLogger();

            // Then
            assertNotNull(logger);
            assertEquals(CliCommand.class.getName(), logger.getName());
        }

        @Test
        @DisplayName("Should use consistent logger across instances")
        void shouldUseConsistentLoggerAcrossInstances() {
            // Given
            TestCliCommand command1 = new TestCliCommand();
            TestCliCommand command2 = new TestCliCommand();

            // When
            Logger logger1 = command1.getLogger();
            Logger logger2 = command2.getLogger();

            // Then
            assertSame(logger1, logger2);
            assertEquals(logger1.getName(), logger2.getName());
        }

        @Test
        @DisplayName("Should provide logger for subclass logging")
        void shouldProvideLoggerForSubclassLogging() {
            // Given
            LoggingTestCommand loggingCommand = new LoggingTestCommand();

            // When
            loggingCommand.executeWithLogging(new String[]{"test"});

            // Then
            assertTrue(loggingCommand.loggedExecution);
        }
    }

    @Nested
    @DisplayName("Inheritance Pattern Tests")
    class InheritancePatternTests {

        @Test
        @DisplayName("Should support multiple concrete implementations")
        void shouldSupportMultipleConcreteImplementations() {
            // Given
            TestCliCommand command1 = new TestCliCommand();
            AnotherTestCommand command2 = new AnotherTestCommand();

            // When
            String name1 = command1.getName();
            String name2 = command2.getName();
            String help1 = command1.getHelp();
            String help2 = command2.getHelp();

            // Then
            assertEquals("test-command", name1);
            assertEquals("another-command", name2);
            assertNotEquals(help1, help2);
        }

        @Test
        @DisplayName("Should allow polymorphic usage")
        void shouldAllowPolymorphicUsage() {
            // Given
            CliCommand[] commands = {
                new TestCliCommand(),
                new AnotherTestCommand(),
                new LoggingTestCommand()
            };

            // When & Then
            for (CliCommand command : commands) {
                assertNotNull(command.getName());
                assertNotNull(command.getHelp());
                assertDoesNotThrow(() -> command.execute(new String[]{"test"}));
            }
        }

        @Test
        @DisplayName("Should support command registration pattern")
        void shouldSupportCommandRegistrationPattern() {
            // Given
            java.util.Map<String, CliCommand> commandRegistry = new java.util.HashMap<>();
            TestCliCommand testCommand = new TestCliCommand();
            AnotherTestCommand anotherCommand = new AnotherTestCommand();

            // When
            commandRegistry.put(testCommand.getName(), testCommand);
            commandRegistry.put(anotherCommand.getName(), anotherCommand);

            // Then
            assertEquals(2, commandRegistry.size());
            assertTrue(commandRegistry.containsKey("test-command"));
            assertTrue(commandRegistry.containsKey("another-command"));
            assertSame(testCommand, commandRegistry.get("test-command"));
            assertSame(anotherCommand, commandRegistry.get("another-command"));
        }
    }

    @Nested
    @DisplayName("Command Interface Contract Tests")
    class CommandInterfaceContractTests {

        @Test
        @DisplayName("Should define required abstract methods")
        void shouldDefineRequiredAbstractMethods() {
            // Given
            Class<CliCommand> commandClass = CliCommand.class;

            // When
            java.lang.reflect.Method[] methods = commandClass.getDeclaredMethods();

            // Then
            boolean hasExecute = java.util.Arrays.stream(methods)
                .anyMatch(method -> method.getName().equals("execute") &&
                         java.lang.reflect.Modifier.isAbstract(method.getModifiers()));
            boolean hasGetName = java.util.Arrays.stream(methods)
                .anyMatch(method -> method.getName().equals("getName") &&
                         java.lang.reflect.Modifier.isAbstract(method.getModifiers()));
            boolean hasGetHelp = java.util.Arrays.stream(methods)
                .anyMatch(method -> method.getName().equals("getHelp") &&
                         java.lang.reflect.Modifier.isAbstract(method.getModifiers()));

            assertTrue(hasExecute, "execute method should be abstract");
            assertTrue(hasGetName, "getName method should be abstract");
            assertTrue(hasGetHelp, "getHelp method should be abstract");
        }

        @Test
        @DisplayName("Should be an abstract class")
        void shouldBeAnAbstractClass() {
            // When
            boolean isAbstract = java.lang.reflect.Modifier.isAbstract(CliCommand.class.getModifiers());

            // Then
            assertTrue(isAbstract);
        }

        @Test
        @DisplayName("Should provide protected logger access")
        void shouldProvideProtectedLoggerAccess() {
            // Given
            Class<CliCommand> commandClass = CliCommand.class;

            // When
            java.lang.reflect.Field loggerField;
            try {
                loggerField = commandClass.getDeclaredField("LOGGER");
            } catch (NoSuchFieldException e) {
                fail("LOGGER field should exist");
                return;
            }

            // Then
            assertTrue(java.lang.reflect.Modifier.isStatic(loggerField.getModifiers()));
            assertTrue(java.lang.reflect.Modifier.isFinal(loggerField.getModifiers()));
            assertTrue(java.lang.reflect.Modifier.isProtected(loggerField.getModifiers()));
            assertEquals(Logger.class, loggerField.getType());
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle exceptions in execute method")
        void shouldHandleExceptionsInExecuteMethod() {
            // Given
            ExceptionThrowingCommand command = new ExceptionThrowingCommand();

            // When & Then
            assertThrows(RuntimeException.class, () -> command.execute(new String[]{"fail"}));
        }

        @Test
        @DisplayName("Should allow graceful error handling in implementations")
        void shouldAllowGracefulErrorHandlingInImplementations() {
            // Given
            GracefulErrorCommand command = new GracefulErrorCommand();

            // When
            command.execute(new String[]{"error"});

            // Then
            assertTrue(command.errorHandled);
        }
    }

    // Test implementation classes
    private static class TestCliCommand extends CliCommand {
        boolean executeWasCalled = false;
        String[] receivedArgs;

        @Override
        public void execute(String[] args) {
            executeWasCalled = true;
            receivedArgs = args;
        }

        @Override
        public String getName() {
            return "test-command";
        }

        @Override
        public String getHelp() {
            return "Test command help text";
        }

        public Logger getLogger() {
            return LOGGER;
        }
    }

    private static class AnotherTestCommand extends CliCommand {
        @Override
        public void execute(String[] args) {
            // Different implementation
        }

        @Override
        public String getName() {
            return "another-command";
        }

        @Override
        public String getHelp() {
            return "Another command help text";
        }
    }

    private static class LoggingTestCommand extends CliCommand {
        boolean loggedExecution = false;

        @Override
        public void execute(String[] args) {
            // Standard execution
        }

        public void executeWithLogging(String[] args) {
            LOGGER.info("Executing command with args: " + java.util.Arrays.toString(args));
            loggedExecution = true;
            execute(args);
        }

        @Override
        public String getName() {
            return "logging-command";
        }

        @Override
        public String getHelp() {
            return "Logging test command";
        }
    }

    private static class ExceptionThrowingCommand extends CliCommand {
        @Override
        public void execute(String[] args) {
            throw new RuntimeException("Command execution failed");
        }

        @Override
        public String getName() {
            return "exception-command";
        }

        @Override
        public String getHelp() {
            return "Command that throws exceptions";
        }
    }

    private static class GracefulErrorCommand extends CliCommand {
        boolean errorHandled = false;

        @Override
        public void execute(String[] args) {
            try {
                if (args != null && args.length > 0 && "error".equals(args[0])) {
                    throw new RuntimeException("Simulated error");
                }
            } catch (RuntimeException e) {
                errorHandled = true;
                LOGGER.warning("Handled error gracefully: " + e.getMessage());
            }
        }

        @Override
        public String getName() {
            return "graceful-error-command";
        }

        @Override
        public String getHelp() {
            return "Command with graceful error handling";
        }
    }
}
