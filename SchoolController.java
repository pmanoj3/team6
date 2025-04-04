package com.example.demo.controller;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.model.Student;

import jakarta.annotation.PostConstruct;

@Controller
public class SchoolController {

    private static final Logger logger = LoggerFactory.getLogger(SchoolController.class);

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
    }

    @PostConstruct
    public void init() {
        testDatabaseConnection();
    }

    private void testDatabaseConnection() {
        try (Connection conn = getConnection()) {
            if (conn != null) {
                logger.info("✅ Database Connected Successfully!");
            }
        } catch (SQLException e) {
            logger.error("❌ Database Connection Failed!", e);
        }
    }

    // Display all students (with search support)
    @GetMapping("/")
    public String home(@RequestParam(value = "search", required = false) String search, Model model) {
        List<Student> students = (search != null && !search.isEmpty()) ? searchStudents(search) : getAllStudents();
        model.addAttribute("students", students);
        return "index";
    }

    // Fetch all students
    private List<Student> getAllStudents() {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT * FROM student";

        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                students.add(mapResultSetToStudent(resultSet));
            }
        } catch (SQLException e) {
            logger.error("❌ Error fetching students from database", e);
        }
        return students;
    }

    // Search students
    private List<Student> searchStudents(String keyword) {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT * FROM student WHERE name LIKE ? OR email LIKE ? OR studentclass LIKE ?";

        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            String searchPattern = "%" + keyword + "%";
            statement.setString(1, searchPattern);
            statement.setString(2, searchPattern);
            statement.setString(3, searchPattern);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    students.add(mapResultSetToStudent(resultSet));
                }
            }
        } catch (SQLException e) {
            logger.error("❌ Error searching students in database", e);
        }
        return students;
    }

    // Show add student form
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("student", new Student());
        return "add";
    }

    // Add student and show success message
   @PostMapping("/add")
public String addStudent(@ModelAttribute Student student, RedirectAttributes redirectAttributes) {
    String sql = "INSERT INTO student (name, email, mobile, studentclass) VALUES (?, ?, ?, ?)";

    try (Connection conn = getConnection();
         PreparedStatement statement = conn.prepareStatement(sql)) {
        statement.setString(1, student.getName());
        statement.setString(2, student.getEmail());
        statement.setString(3, student.getMobile());
        statement.setString(4, student.getStudentClass());
        statement.executeUpdate();

        redirectAttributes.addFlashAttribute("successMessage", "Student added successfully!");
        return "redirect:/";
    } catch (SQLException e) {
        logger.error("❌ Error adding student to database", e);
        redirectAttributes.addFlashAttribute("errorMessage", "Database error occurred during add.");
    }
    return "redirect:/";
}


    // Show edit form
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Student student = getStudentById(id);
        if (student == null) {
            logger.error("❌ Student not found with ID: " + id);
            return "redirect:/";
        }
        model.addAttribute("student", student);
        return "edit";
    }

    // Update student and show success message
    @PostMapping("/update")
    public String updateStudent(@ModelAttribute Student student, Model model) {
        String sql = "UPDATE student SET name=?, email=?, mobile=?, studentclass=? WHERE id=?";

        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, student.getName());
            statement.setString(2, student.getEmail());
            statement.setString(3, student.getMobile());
            statement.setString(4, student.getStudentClass());
            statement.setLong(5, student.getId());
            
            int rowsUpdated = statement.executeUpdate();
            if (rowsUpdated > 0) {
                model.addAttribute("successMessage", "Student updated successfully!");
            } else {
                model.addAttribute("errorMessage", "Failed to update student. Please try again.");
            }
        } catch (SQLException e) {
            logger.error("❌ Error updating student", e);
            model.addAttribute("errorMessage", "Database error occurred during update.");
        }
        
        return "redirect:/"; // Redirect to home after update
    }

    // Delete student and show success message
    @GetMapping("/delete/{id}")
    public String deleteStudent(@PathVariable Long id, Model model) {
        String sql = "DELETE FROM student WHERE id=?";

        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
            model.addAttribute("successMessage", "Student deleted successfully!");
        } catch (SQLException e) {
            logger.error("❌ Error deleting student", e);
            model.addAttribute("errorMessage", "Database error occurred during deletion.");
        }
        return "redirect:/";
    }

    // Fetch student by ID
    private Student getStudentById(Long id) {
        Student student = null;
        String sql = "SELECT * FROM student WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    student = mapResultSetToStudent(resultSet);
                }
            }
        } catch (SQLException e) {
            logger.error("❌ Error fetching student by ID", e);
        }
        return student;
    }

    // Map ResultSet to Student object
    private Student mapResultSetToStudent(ResultSet resultSet) throws SQLException {
        Student student = new Student();
        student.setId(resultSet.getLong("id"));
        student.setName(resultSet.getString("name"));
        student.setEmail(resultSet.getString("email"));
        student.setMobile(resultSet.getString("mobile"));
        student.setStudentClass(resultSet.getString("studentclass"));
        return student;
    }
} 