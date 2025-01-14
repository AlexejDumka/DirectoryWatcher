    234567OP/89.# Directory Watcher and PDF Converter

This project is a Java-based application that continuously monitors a specified directory for new PDF files. When a new PDF file is detected, the application converts the PDF into a text file and saves it in a different output directory. After successful conversion, the original PDF file is deleted to keep the input directory clean. The project uses virtual threads to handle tasks efficiently and employs logging for easy tracking of operations.r.

***
## Features
- **Directory Monitoring:** Watches a specified input directory for any new, modified, or deleted PDF files.
- **PDF Cleanup:** Deletes the original PDF files after successful conversion.
- **Centralized Error Handling:** Logs all errors and events to help with debugging and monitoring.
- **Virtual Threads:** Utilizes Java's virtual threads for efficient task management.

***
## Prerequisites
- Java 21 or higher (to support virtual threads)
- [Apache Maven](https://maven.apache.org/) (for building and running the project)
- [PDFBox library](https://pdfbox.apache.org/) (already configured in the project dependencies)


***
## Installation
#### Clone the repository:

```
git clone https://github.com/alexej.dumka/directory-watcher-pdf-converter.git
cd directory-watcher-pdf-converter
```

#### Build the project using Maven:
```
mvn clean package
```
***
## Setup
Ensure you have two directories created within the project root:
**IN** - This is where you place PDF files for conversion.
**OUT** - Converted text files will be saved here.
If these directories do not exist, the application will create them automatically.
> Note: Make sure`--Java 21 or higher` is installed and available on your system's PATH.

***
## Running the Application
After building the project, run the application using the following command:

```
java -cp target/directory-watcher-1.0-SNAPSHOT.jar com.demo.DirectoryWatcherTask
```

#### Example of usage:

1. Place a PDF file inside the IN directory.
2. The application will detect the new file, convert it to a .txt file, and save it in the OUT directory.
3. Once converted, the original PDF file will be deleted from the IN directory.

***
## Logging
The application uses Java's built-in logging system. Logs will provide information on the status of file conversions, errors, and any other significant events.
