package com.example.bankingsystem
import androidx.activity.compose.setContent
import androidx.compose.material.Typography


import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.ui.unit.dp import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.*

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.compose.ui.unit.dp


import androidx.activity.compose.setContent
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


fun prepopulateDatabase(context: Context) {
    val db = AppDatabase.getDatabase(context)
    val customerDao = db.customerDao()

    CoroutineScope(Dispatchers.IO).launch {
        if (customerDao.getAllCustomers().isEmpty()) {
            val customers = listOf(
                Customer(name = "John Doe", email = "john@gmail.com", balance = 1000.0),
                Customer(name = "Jane Smith", email = "jane@gmail.com", balance = 2000.0),
                Customer(name = "Robert Brown", email = "robert@gmail.com", balance = 1500.0),
                Customer(name = "Emily White", email = "emily@gmail.com", balance = 3000.0),
                Customer(name = "Michael Green", email = "michael@gmail.com", balance = 500.0),
                Customer(name = "Sarah Black", email = "sarah@gmail.com", balance = 700.0),
                Customer(name = "David Blue", email = "david@gmail.com", balance = 1200.0),
                Customer(name = "Laura Red", email = "laura@gmail.com", balance = 900.0),
                Customer(name = "James Yellow", email = "james@gmail.com", balance = 1100.0),
                Customer(name = "Anna Pink", email = "anna@gmail.com", balance = 2500.0)
            )
            customers.forEach { customerDao.insertCustomer(it) }
        }
    }
}

class CustomerRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)

    suspend fun getAllCustomers(): List<Customer> {
        return db.customerDao().getAllCustomers()
    }

    suspend fun deleteAllTransactions() {
        db.transferDao().deleteAllTransfers()
    }

    suspend fun getCustomerById(id: Int): Customer? {
        return db.customerDao().getCustomerById(id)
    }

    suspend fun transferMoney(fromCustomerId: Int, toCustomerId: Int, amount: Double) {
        val fromCustomer = db.customerDao().getCustomerById(fromCustomerId)
        val toCustomer = db.customerDao().getCustomerById(toCustomerId)
        if (fromCustomer != null && toCustomer != null && fromCustomer.balance >= amount) {
            fromCustomer.balance -= amount
            toCustomer.balance += amount
            db.customerDao().updateCustomer(fromCustomer)
            db.customerDao().updateCustomer(toCustomer)
            db.transferDao().insertTransfer(
                Transfer(
                    fromCustomerId = fromCustomerId,
                    toCustomerId = toCustomerId,
                    amount = amount,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun getAllTransfers(): List<Transfer> {
        return db.transferDao().getAllTransfers()
    }

    suspend fun getCustomerNamesByIds(ids: List<Int>): Map<Int, String> {
        val customers = db.customerDao().getCustomersByIds(ids)
        return customers.associateBy({ it.id }, { it.name })
    }
}

class CustomerViewModel(context: Context) : ViewModel() {
    private val repository = CustomerRepository(context)

    val customers = mutableStateListOf<Customer>()

    fun loadCustomers() {
        viewModelScope.launch {
            customers.clear()
            customers.addAll(repository.getAllCustomers())
        }
    }

    fun deleteAllTransactions(transactions: MutableList<TransferWithNames>) {
        viewModelScope.launch {
            repository.deleteAllTransactions()
            transactions.clear()
        }
    }

    fun getCustomerById(id: Int): Customer? {
        return customers.find { it.id == id }
    }

    fun loadCustomer(id: Int) {
        viewModelScope.launch {
            val customer = repository.getCustomerById(id)
            customer?.let {
                customers.removeIf { it.id == id }
                customers.add(it)
            }
        }
    }

    fun transferMoney(fromCustomerId: Int, toCustomerId: Int, amount: Double) {
        viewModelScope.launch {
            repository.transferMoney(fromCustomerId, toCustomerId, amount)
            loadCustomers()
        }
    }

    fun loadTransactionsWithCustomerNames(transactions: MutableList<TransferWithNames>) {
        viewModelScope.launch {
            val allTransfers = repository.getAllTransfers()
            val customerIds = allTransfers.flatMap { listOf(it.fromCustomerId, it.toCustomerId) }.distinct()
            val customerNames = repository.getCustomerNamesByIds(customerIds)
            transactions.clear()
            transactions.addAll(allTransfers.map {
                TransferWithNames(
                    fromCustomerName = customerNames[it.fromCustomerId] ?: "Unknown",
                    toCustomerName = customerNames[it.toCustomerId] ?: "Unknown",
                    amount = it.amount
                )
            })
        }
    }
}

data class TransferWithNames(
    val fromCustomerName: String,
    val toCustomerName: String,
    val amount: Double
)

// UI and Navigation
@Composable
fun NavGraph(innerPadding: PaddingValues, navController: NavHostController = rememberNavController(), viewModel: CustomerViewModel, startDestination: String = "home") {
    NavHost(navController, startDestination) {
        composable("home") { HomeScreen(navController) }
        composable("customers") { CustomerListScreen(navController, viewModel) }
        composable("customer/{id}") { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString("id")?.toInt() ?: 0
            CustomerDetailScreen(navController, customerId, viewModel)
        }
        composable("transfer/{fromId}") { backStackEntry ->
            val fromCustomerId = backStackEntry.arguments?.getString("fromId")?.toInt() ?: 0
            TransferScreen(navController, fromCustomerId, viewModel)
        }
        composable("transactions") { TransactionsScreen(viewModel) }
    }
}

@Composable
fun HomeScreen(navController: NavHostController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFBBDEFB),
                        Color(0xFF2196F3)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Welcome to MyBank",
                style = MaterialTheme.typography.h4.copy( // Use typography.h4 for larger text
                    color = Color.Blue,  // Set color to blue
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.0f)  // Adjust the aspect ratio as needed
                    .padding(6.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.bank1),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.height(30.dp)) // Spacer with reduced height
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { navController.navigate("customers") },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF2196F3)
                    ),
                    elevation = ButtonDefaults.buttonElevation(8.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("View All Customers", style = MaterialTheme.typography.h6)
                }
                Spacer(modifier = Modifier.height(50.dp)) // Reduced height between buttons
                Button(
                    onClick = { navController.navigate("transactions") },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF2196F3)
                    ),
                    elevation = ButtonDefaults.buttonElevation(8.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("View Transactions", style = MaterialTheme.typography.h6)
                }
            }
        }
    }
}

@Composable
fun TransactionsScreen(viewModel: CustomerViewModel = viewModel()) {
    val transactions = remember { mutableStateListOf<TransferWithNames>() }

    LaunchedEffect(Unit) {
        viewModel.loadTransactionsWithCustomerNames(transactions)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFBBDEFB), // Light blue color
                        Color(0xFF2196F3)  // Darker blue color
                    )
                )
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Transactions",
            style = MaterialTheme.typography.h4,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(transactions) { transaction ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center // Align transaction text in the center
                ) {
                    TransactionItem(transaction = transaction)
                }
            }
        }

        Button(
            onClick = { viewModel.deleteAllTransactions(transactions) },
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp)
        ) {
            Text("Delete All Transactions")
        }
    }
}

@Composable
fun TransactionItem(transaction: TransferWithNames) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = "From: ${transaction.fromCustomerName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "To: ${transaction.toCustomerName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Amount: \$${transaction.amount}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
    }
}



@Composable
fun CustomerListScreen(navController: NavHostController, viewModel: CustomerViewModel = viewModel()) {
    val customers = remember { viewModel.customers }

    LaunchedEffect(Unit) {
        viewModel.loadCustomers()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFBBDEFB),
                            Color(0xFF2196F3)
                        )
                    )
                )
        )

        LazyColumn(
            modifier = Modifier.padding(16.dp)
        ) {
            items(customers) { customer ->
                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .clickable { navController.navigate("customer/${customer.id}") }
                        .fillMaxWidth()
                        .background(
                            color =   Color.White,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold,
                        color = Color.Blue, // Set text color to blue
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
    }
}


@Composable
fun CustomerDetailScreen(navController: NavHostController, customerId: Int, viewModel: CustomerViewModel = viewModel()) {
    val customer = remember { viewModel.getCustomerById(customerId) }

    LaunchedEffect(customerId) {
        viewModel.loadCustomer(customerId)
    }

    customer?.let {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFBBDEFB),
                            Color(0xFF2196F3)
                        )
                    )
                )
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter // Align content to the top
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp)) // Add space between image and text

                Text(
                    text = "Customer Details",
                    style = MaterialTheme.typography.h4,
                    color = Color.Black, // Change text color to black
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Image(
                    painter = painterResource(id = R.drawable.customer), // Replace R.drawable.customer_image with your image resource
                    contentDescription = null,
                    modifier = Modifier
                        .size(180.dp) // Increase image size
                        .padding(bottom = 16.dp)
                        .clip(shape = RoundedCornerShape(8.dp))
                )

                Spacer(modifier = Modifier.height(16.dp)) // Add space between image and details

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, shape = RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(16.dp) // Increase space between text items
                    ) {
                        Text(
                            text = "Name:",
                            style = MaterialTheme.typography.subtitle1.copy(fontSize = 18.sp), // Increase text size
                            color = Color.Black
                        )
                        Text(
                            text = it.name,
                            style = MaterialTheme.typography.body1.copy(fontSize = 16.sp), // Increase text size
                            color = Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp)) // Add space between detail boxes

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, shape = RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(16.dp) // Increase space between text items
                    ) {
                        Text(
                            text = "Email:",
                            style = MaterialTheme.typography.subtitle1.copy(fontSize = 18.sp), // Increase text size
                            color = Color.Black
                        )
                        Text(
                            text = it.email,
                            style = MaterialTheme.typography.body1.copy(fontSize = 16.sp), // Increase text size
                            color = Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp)) // Add space between detail boxes

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, shape = RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(16.dp) // Increase space between text items
                    ) {
                        Text(
                            text = "Balance:",
                            style = MaterialTheme.typography.subtitle1.copy(fontSize = 18.sp), // Increase text size
                            color = Color.Black
                        )
                        Text(
                            text = "${it.balance}",
                            style = MaterialTheme.typography.body1.copy(fontSize = 16.sp), // Increase text size
                            color = Color.Black
                        )
                    }
                }

                Button(
                    onClick = { navController.navigate("transfer/$customerId") },
                    modifier = Modifier.padding(top = 32.dp) // Increase top padding
                ) {
                    Text(
                        "Transfer Money",
                        fontSize = 18.sp, // Increase button text size
                        fontWeight = FontWeight.Bold // Make button text bold
                    )
                }
            }
        }
    }
}
@Composable
fun TransferScreen(navController: NavHostController, fromCustomerId: Int, viewModel: CustomerViewModel = viewModel()) {
    val customers = remember { viewModel.customers }
    val coroutineScope = rememberCoroutineScope()
    var amount by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadCustomers()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFBBDEFB),
                        Color(0xFF2196F3)
                    )
                )
            )
            .padding(16.dp) // Add padding to the whole column
    ) {
        Text(
            text = "AMOUNT TO TRANSFER",
            style = MaterialTheme.typography.body1.copy(fontSize = 25.sp), // Adjust text size
            color = Color.Black, // Adjust text color
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 8.dp) // Add vertical padding to the label
        )

        TextField(
            value = amount,
            onValueChange = { amount = it },
            label = { },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 8.dp) // Add vertical padding to the input field
        )

        LazyColumn(
            modifier = Modifier.padding(vertical = 16.dp) // Add vertical padding between items
        ) {
            items(customers) { customer ->
                if (customer.id != fromCustomerId) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp) // Add vertical padding to each box
                            .background(Color.White, shape = RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = customer.name,
                                modifier = Modifier.padding(horizontal = 16.dp) // Add horizontal padding to the text
                            )

                            // Use a Box to ensure the button fits within its parent Box
                            Box(modifier = Modifier.padding(end = 16.dp)) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val transferAmount = amount.toDoubleOrNull()
                                            if (transferAmount != null) {
                                                viewModel.transferMoney(fromCustomerId, customer.id, transferAmount)
                                                navController.navigate("home")  // Navigate back to home after transaction
                                            }
                                        }
                                    }
                                ) {
                                    Text("Transfer Money")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}




class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prepopulateDatabase(this)  // Prepopulate the database

        val customerViewModelFactory = CustomerViewModelFactory(applicationContext)

        setContent {
            BankingsystemTheme {
                val navController = rememberNavController()
                val customerViewModel: CustomerViewModel = viewModel(factory = customerViewModelFactory)
                Scaffold { innerPadding ->
                    NavGraph(innerPadding, navController, customerViewModel)
                }
            }
        }
    }
}

class CustomerViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CustomerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CustomerViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class App : Application() {
    init {
        instance = this
    }

    companion object {
        lateinit var instance: App
            private set
    }
}

@Composable
fun BankingsystemTheme(content: @Composable () -> Unit) {
    MaterialTheme {
        content()
    }
}

