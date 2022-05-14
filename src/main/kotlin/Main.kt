import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.io.File
import java.io.FileNotFoundException
import java.io.FileWriter
import java.io.IOException
import kotlin.system.exitProcess

fun readFromDb(filePath: String): MutableList<String> {
    val tasks: MutableList<String> = mutableListOf()
    val lines = File(filePath).readLines()
    for (line in lines) {
        tasks.add(line)
    }

    return tasks
}

fun writeToDb(filePath: String, text: String) {
    val fw = FileWriter(filePath)
    fw.write(text)
    fw.close()
}

fun main() = application {
    val fr = javax.swing.JFileChooser()
    val fsv = fr.fileSystemView
    val cwd = fsv.defaultDirectory.toString()
    var tasks: MutableList<String> = mutableListOf()
    val db = "$cwd/db"
    var errorNotification = ""
    try {
        tasks = readFromDb(db)
    } catch (e: FileNotFoundException) {
        errorNotification = "File could not be found, created a new one!"
        try {
            writeToDb(db, "Dummy Task")
        } catch (e: IOException) {
            errorNotification = "Could not write new data"
        } catch (e: Exception) {
            println("Unknown file write error: $e")
            exitProcess(1)
        }
    } catch (e: Exception) {
        println("Unknown file read error: $e")
        exitProcess(1)
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Todo App",
        state = rememberWindowState(width = 500.dp, height = 500.dp),
        resizable = true,
    ) {
        val todoInputText = remember { mutableStateOf("") }
        val todos = remember { mutableStateOf(tasks) }
        val notification = remember { mutableStateOf(errorNotification) }

        MaterialTheme {
            Column(Modifier.fillMaxSize(), Arrangement.spacedBy(5.dp)) {
                if (notification.value.isNotEmpty()) {
                    Text(
                        notification.value,
                        modifier = Modifier.padding(10.dp),
                        color = Color.Red,
                    )
                }

                Row {
                    TextField(
                        value = todoInputText.value,
                        modifier = Modifier.padding(10.dp),
                        label = { Text("Todo text") },
                        onValueChange = { text: String -> todoInputText.value = text },
                    )

                    Button(
                        modifier = Modifier.padding(10.dp),
                        onClick = {
                            notification.value = ""
                            if (todoInputText.value.trim().isEmpty()) {
                                notification.value = "NOTIFICATION: You need to enter a valid value"
                            } else if (todoInputText.value in todos.value) {
                                notification.value = "NOTIFICATION: You already have the same task"
                            } else {
                                todos.value += todoInputText.value
                                var text = ""
                                todos.value.map { todo -> text += todo + "\n" }

                                try {
                                    writeToDb(db, text)
                                    todoInputText.value = ""
                                } catch (e: Exception) {
                                    notification.value = "NOTIFICATION: Could not save to database"
                                }
                            }
                        }) {
                        Text("Add")
                    }
                }

                for (todo in todos.value) {
                    Row {
                        Text(
                            todo,
                            modifier = Modifier.padding(15.dp)
                        )

                        Button(
                            onClick = {
                                notification.value = ""
                                todos.value
                                val allowedTodos: MutableList<String> = mutableListOf()

                                todos.value.map { selectedTodo ->
                                    if (selectedTodo != todo) {
                                        allowedTodos.add(selectedTodo)
                                    }
                                }

                                var text = ""
                                for (selectedTodo in allowedTodos) {
                                    text += selectedTodo + "\n"
                                }

                                try {
                                    writeToDb(db, text)
                                    todos.value = allowedTodos
                                } catch (e: Exception) {
                                    notification.value = "NOTIFICATION: Could not save to database"
                                }

                            }) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}