package dev.balakumar.promptforge.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

@Service
@State(
    name = "PromptForgeSettings",
    storages = [Storage("promptForgeSettings.xml")]
)
class PromptForgeSettings : PersistentStateComponent<PromptForgeSettings.State> {

    data class State(
        // General settings
        var includeRelatedFiles: Boolean = true,
        var maxRelatedFiles: Int = 5,
        var maxDepth: Int = 1,
        var includeImplementations: Boolean = true,
        var includeJavadoc: Boolean = true,
        var smartPruning: Boolean = true,
        var includeDependencies: Boolean = false,
        var includeDecompiled: Boolean = false,
        var maxDecompiledFiles: Int = 3,
        var skipJavaPackages: Boolean = true,

        // Prompt templates
        var newFilePromptTemplate: String = DEFAULT_NEW_FILE_PROMPT,
        var modifiedFilePromptTemplate: String = DEFAULT_MODIFIED_FILE_PROMPT,
        var explainCodePromptTemplate: String = DEFAULT_EXPLAIN_CODE_PROMPT,
        var askQuestionPromptTemplate: String = DEFAULT_ASK_QUESTION_PROMPT,
        var makeChangePromptTemplate: String = DEFAULT_MAKE_CHANGE_PROMPT,

        // Excluded packages
        var excludedPackages: List<String> = DEFAULT_EXCLUDED_PACKAGES
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    companion object {
        val DEFAULT_EXPLAIN_CODE_PROMPT = """I'm looking at a Java class called {CLASS_NAME} and would like you to help me understand it.

Please analyze the code and:
1. Explain the purpose and functionality of the class
2. Describe key methods and their roles
3. Identify any design patterns or notable coding practices
4. Explain any complex or non-obvious parts

Here's the class:
{CLASS_CONTENT}
{RELATED_FILES_CONTENT}

Please provide a comprehensive explanation that would help me understand this code better.
"""

        val DEFAULT_ASK_QUESTION_PROMPT = """I have a question about this Java class called {CLASS_NAME}:

{QUESTION}

Here's the class:
{CLASS_CONTENT}
{RELATED_FILES_CONTENT}

Please provide a detailed answer to my question based on the code.
"""

        val DEFAULT_MAKE_CHANGE_PROMPT = """I need to make the following change to this Java class called {CLASS_NAME}:

{CHANGE_REQUEST}

Here's the current code:
{CLASS_CONTENT}
{RELATED_FILES_CONTENT}

Please provide:
1. The modified code with the requested changes
2. An explanation of what you changed and why
3. Any potential issues or considerations I should be aware of
"""

        val DEFAULT_NEW_FILE_PROMPT = """Your task is to write a complete JUnit 5 test class for {CLASS_NAME}.

IMPORTANT: Your response should ONLY contain the Java code for the test class, nothing else.

## Testing Guidelines

Follow these rules strictly:

1. Start with the package declaration (if applicable).
2. Include all necessary import statements, including JUnit 5 and Mockito.
3. Write the full test class declaration with appropriate annotations.
4. Create mock objects for all dependencies using @Mock annotation.
5. Implement a @BeforeEach method to initialize mocks and set up common test scenarios.
6. Include multiple test methods to cover various scenarios, including:
   - Happy path tests
   - Edge cases
   - Error handling scenarios
7. Use appropriate JUnit 5 annotations like @Test, @DisplayName, etc.
8. Implement thorough mocking, including:
   - Stubbing method calls with when().thenReturn()
   - Mocking void methods with doNothing().when() if necessary
   - Mocking exceptions with when().thenThrow()
9. Use Mockito's verify() to ensure methods are called with correct parameters and frequency.
10. Include tests for all public methods of the class under test.
11. Pay special attention to null checks and conditional logic in the original class.
12. Use assertThrows() to test exception scenarios correctly:
    - For direct method calls, assert the expected exception type
    - For indirect calls (reflection, proxies, etc.), assert the appropriate wrapper exception and verify its cause
13. Add comments to clearly separate Arrange, Act, and Assert sections in each test method.
14. Aim for at least 90% line coverage of the main class.
15. End the file properly with closing braces.
16. DO NOT assume any methods or fields of classes that are not explicitly provided in the context.

## Testing Best Practices

- When testing private methods through reflection, properly handle exception wrapping
- For methods that return collections, verify both the collection and its contents
- Test boundary conditions for numeric parameters
- For methods with complex logic, test each branch of conditional statements
- Ensure proper resource cleanup in tests that acquire resources

Here's the class to test:

{CLASS_CONTENT}

{RELATED_FILES_CONTENT}

DO NOT include any explanations, comments, or anything other than the Java code itself. Begin the Java file content immediately, starting with the package or import statements.
"""

        val DEFAULT_MODIFIED_FILE_PROMPT = """I'll provide you with:

1. An original Java class
2. A diff showing changes to that class
3. The current test file
4. Related files (if applicable)

Your task is to write ONLY the new or modified JUnit 5 test methods needed to cover the changes.

IMPORTANT: Your response should ONLY contain the Java code for the test methods, nothing else.

## Testing Guidelines

1. Only generate test methods - no class declarations, imports, or setup code
2. Cover all new/modified code in the diff
3. Include tests for:
   - Happy path scenarios
   - Edge cases
   - Error handling
4. Use appropriate JUnit 5 annotations (@Test, @DisplayName, etc.)
5. Implement thorough mocking as needed
6. Use Mockito's verify() to ensure methods are called correctly
7. Follow AAA pattern (Arrange, Act, Assert) with comments
8. Match the style of the existing test file
9. DO NOT modify existing test methods unless necessary

## Special Testing Considerations

### Exception Testing
- When testing methods that might throw exceptions, ensure you're asserting for the correct exception type
- For indirect method calls (including reflection, proxies, or async operations), remember that exceptions may be wrapped in other exception types
- Always verify both the exception type and message for complete test coverage

### Method Access Testing
- When testing non-public methods, ensure you're using the appropriate technique based on the existing test patterns
- Consider if the method can be tested through public interfaces instead

### Asynchronous Code
- Use appropriate timeouts and assertions for async operations
- Consider using awaitility or similar libraries for complex async tests

### Resource Cleanup
- Ensure proper cleanup in tests using try-finally or JUnit's resource management

Here's the original class:

{ORIGINAL_CLASS}

Here's the diff showing changes:

{DIFF}

Here's the current test file:

{TEST_FILE}

{RELATED_FILES_CONTENT}

Generate only the test methods needed to cover the changes.
"""

        val DEFAULT_EXCLUDED_PACKAGES = listOf(
            "java.lang",
            "java.util",
            "java.io",
            "java.net",
            "java.math",
            "java.time",
            "java.text",
            "java.sql",
            "java.awt",
            "java.applet",
            "java.beans",
            "java.nio",
            "java.rmi",
            "java.security",
            "javax.swing",
            "javax.servlet",
            "javax.ejb",
            "javax.persistence",
            "javax.xml",
            "org.w3c",
            "org.xml",
            "com.sun",
            "sun.",
            "kotlin.",
            "org.jetbrains",
            "org.springframework",
            "org.apache",
            "com.google",
            "com.fasterxml"
        )

        fun getInstance(): PromptForgeSettings = service()
    }
}