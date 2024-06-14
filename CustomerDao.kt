package com.example.bankingsystem
import androidx.room.*

@Dao
interface CustomerDao {
    @Insert
    suspend fun insertCustomer(customer: Customer)

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Query("SELECT * FROM customer")
    suspend fun getAllCustomers(): List<Customer>

    @Query("SELECT * FROM customer WHERE id = :id")
    suspend fun getCustomerById(id: Int): Customer?

    @Query("SELECT * FROM customer WHERE id IN (:ids)")
    suspend fun getCustomersByIds(ids: List<Int>): List<Customer>
}
