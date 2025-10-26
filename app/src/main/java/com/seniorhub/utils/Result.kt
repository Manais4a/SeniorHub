package com.seniorhub.utils

/**
 * Result - Generic Result Wrapper for Asynchronous Operations
 *
 * This sealed class provides a comprehensive way to handle the results of asynchronous operations
 * in the SeniorHub application, including success, error, and loading states.
 *
 * Features:
 * - Type-safe result handling with sealed class hierarchy
 * - Support for nullable data in success cases
 * - Comprehensive error handling with exception information
 * - Loading state tracking for UI feedback
 * - Functional programming utilities (map, onSuccess, onFailure)
 * - Safe data extraction with getOrNull() and getOrElse()
 *
 * Usage Examples:
 * ```kotlin
 * // Success case
 * val result: Result<User> = Result.Success(user)
 * result.onSuccess { user -> updateUI(user) }
 *
 * // Error case
 * val errorResult: Result<User> = Result.Error(NetworkException())
 * errorResult.onFailure { error -> showError(error.message) }
 *
 * // Loading case
 * val loadingResult: Result<User> = Result.Loading()
 * // Show loading indicator
 * ```
 *
 * @param T The type of data contained in the result
 * @author SeniorHub Team
 * @version 1.0.0
 */
sealed class Result<out T : Any> {
    /**
     * Loading - Represents an in-progress operation
     *
     * This class indicates that an asynchronous operation is currently running
     * and the result is not yet available. Use this state to show loading indicators
     * in the UI while waiting for the operation to complete.
     */
    class Loading<out T : Any> : Result<T>() {
        override fun toString() = "[Loading]"
    }
    /**
     * Success - Represents a successful operation with a result of type [T]
     *
     * This class contains the successful result of an operation. The data field
     * can be null to represent successful operations that don't return data
     * (e.g., delete operations).
     *
     * @param data The successful result data, can be null
     */
    data class Success<out T : Any>(val data: T?) : Result<T>() {
        override fun toString() = "[Success: $data]"
    }

    /**
     * Error - Represents a failed operation with an exception
     *
     * This class contains the exception that caused an operation to fail.
     * Use this to handle errors gracefully and provide appropriate feedback
     * to users.
     *
     * @param exception The exception that caused the operation to fail
     */
    data class Error(val exception: Exception) : Result<Nothing>() {
        override fun toString() = "[Error: $exception]"
    }

    /**
     * Returns the success value if this is a [Success] result, or null otherwise
     *
     * This is a safe way to extract data from a Result without throwing exceptions.
     * Use this when you want to handle null cases explicitly.
     *
     * @return The success data if available, null otherwise
     */
    fun getOrNull(): T? = (this as? Success)?.data

    /**
     * Returns the exception if this is an [Error] result, or null otherwise
     *
     * This is a safe way to extract the exception from a Result without throwing exceptions.
     * Use this when you want to handle error cases explicitly.
     *
     * @return The exception if available, null otherwise
     */
    fun exceptionOrNull(): Exception? = (this as? Error)?.exception

    /**
     * Returns the success value if this is a [Success] result, or throws the exception if it's an [Error]
     * @throws Exception if this is an [Error] result
     */

    /**
     * Returns the success value if this is a [Success] result, or the result of [onFailure] if it's an [Error]
     *
     * This method provides a way to provide a default value or alternative computation
     * when an operation fails. The onFailure lambda receives the exception and should
     * return a value of type T.
     *
     * @param onFailure Lambda that provides an alternative value when the operation fails
     * @return The success data or the result of onFailure
     */
    fun getOrElse(onFailure: (Exception) -> @UnsafeVariance T): Result<T> = when (this) {
        is Success -> Success(data)
        is Error -> Success(onFailure(exception))
        is Loading -> this
    }

    /**
     * Maps the success value using [transform] if this is a [Success] result, or returns this if it's an [Error]
     *
     * This method allows you to transform the success data while preserving the Result structure.
     * If the result is an Error or Loading, it returns the original result unchanged.
     *
     * @param transform Lambda that transforms the success data
     * @return A new Result with the transformed data
     */
    fun <R : Any> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(data?.let { transform(it) })
        is Error -> this
        is Loading<*> -> TODO()
    }

    /**
     * Maps the exception using [transform] if this is an [Error] result, or returns this if it's a [Success]
     *
     * This method allows you to transform the exception while preserving the Result structure.
     * If the result is a Success or Loading, it returns the original result unchanged.
     *
     * @param transform Lambda that transforms the exception
     * @return A new Result with the transformed exception
     */
    fun mapError(transform: (Exception) -> Exception): Result<T> = when (this) {
        is Success -> this
        is Error -> Error(transform(exception))
        is Loading -> this
    }

    /**
     * Executes [action] if this is a [Success] result
     *
     * This method allows you to perform side effects when a result is successful.
     * The action lambda receives the success data and can perform operations like
     * updating the UI or logging.
     *
     * @param action Lambda to execute with the success data
     * @return The original Result for method chaining
     */
    fun onSuccess(action: (T) -> Unit): Result<T> = apply {
        if (this is Success) data?.let { action(it) }
    }

    /**
     * Executes [action] if this is an [Error] result
     *
     * This method allows you to perform side effects when a result is an error.
     * The action lambda receives the exception and can perform operations like
     * showing error messages or logging.
     *
     * @param action Lambda to execute with the exception
     * @return The original Result for method chaining
     */
    fun onFailure(action: (Exception) -> Unit): Result<T> = apply {
        if (this is Error) action(exception)
    }
}
