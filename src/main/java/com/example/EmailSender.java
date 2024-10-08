package com.example;

import javax.swing.*;

import io.github.cdimascio.dotenv.Dotenv;

import javax.mail.*;
import javax.mail.internet.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Properties;

public class EmailSender extends JFrame {
    private JTextField emailField, toField, ccField, dateRangeField;
    private JPasswordField passwordField;
    private JLabel attachmentLinkLabel;
    private JButton sendButton, attachButton;
    private File selectedFile;

    public EmailSender() {
        Dotenv dotenv = Dotenv.load();
        String email = dotenv.get("EMAIL_ADDRESS");
        String password = dotenv.get("EMAIL_PASSWORD");

        // when converting to .exe file, uncomment this
        // String email = System.getProperty("EMAIL_ADDRESS");
        // String password = System.getProperty("EMAIL_PASSWORD");

        setTitle("Email Sender");
        setSize(400, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(null);
        setResizable(false);

        ImageIcon icon = new ImageIcon(getClass().getResource("/images/e-sig.png"));
        setIconImage(icon.getImage());

        JLabel emailLabel = new JLabel("Email Address:");
        emailField = new JTextField(email != null ? email : "");

        JLabel passwordLabel = new JLabel("Password:");
        passwordField = new JPasswordField(password != null ? password : "");

        JLabel toLabel = new JLabel("To:");
        toField = new JTextField();

        JLabel ccLabel = new JLabel("CC (comma-separated):");
        ccField = new JTextField();

        JLabel dateRangeLabel = new JLabel("Date Range:");
        dateRangeField = new JTextField("SEPTEMBER 1-15");

        attachmentLinkLabel = new JLabel("No file attached");
        attachButton = new JButton("Attach File");
        sendButton = new JButton("Send Email");

        JLabel linkLabel = new JLabel("e-sig here");
        linkLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        linkLabel.setBounds(20, 300, 100, 25);
        linkLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        linkLabel.setForeground(Color.blue);
        linkLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openWebPage(EmailConstants.E_SIG_SITE);
            }
        });

        setLayout(null);
        emailLabel.setBounds(20, 20, 100, 25);
        emailField.setBounds(120, 20, 250, 25);
        passwordLabel.setBounds(20, 60, 100, 25);
        passwordField.setBounds(120, 60, 250, 25);
        toLabel.setBounds(20, 100, 100, 25);
        toField.setBounds(120, 100, 250, 25);
        ccLabel.setBounds(20, 140, 150, 25);
        ccField.setBounds(170, 140, 200, 25);
        dateRangeLabel.setBounds(20, 180, 100, 25);
        dateRangeField.setBounds(120, 180, 250, 25);
        linkLabel.setBounds(20, 400, 120, 30);
        attachButton.setBounds(139, 220, 120, 30);
        attachmentLinkLabel.setBounds(150, 260, 250, 25);
        sendButton.setBounds(150, 400, 100, 30);

        add(emailLabel);
        add(emailField);
        add(passwordLabel);
        add(passwordField);
        add(toLabel);
        add(toField);
        add(ccLabel);
        add(ccField);
        add(dateRangeLabel);
        add(dateRangeField);
        add(linkLabel);
        add(attachButton);
        add(attachmentLinkLabel);
        add(sendButton);

        attachButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                chooseFile();
            }
        });

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendEmail();
            }
        });
    }

    private void openWebPage(String uri) {
        try {
            Desktop.getDesktop().browse(new java.net.URI(uri));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void chooseFile() {
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            selectedFile = fileChooser.getSelectedFile();
            attachmentLinkLabel.setText("<html><a href='#'>View Attached PDF</a></html>");
            attachmentLinkLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            attachmentLinkLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    openFileInBrowser(selectedFile);
                }
            });
        }
    }

    private void openFileInBrowser(File file) {
        try {
            Desktop.getDesktop().browse(file.toURI());
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error opening file in browser: " + e.getMessage());
        }
    }

    private void sendEmail() {
        String email = emailField.getText();
        String to = toField.getText().trim();
        String cc = ccField.getText().trim();
        String dateRange = dateRangeField.getText();
        String password = new String(passwordField.getPassword());
        String subject = String.format(EmailConstants.SUBJECT_TEMPLATE, dateRange);
        String body = String.format(EmailConstants.BODY_TEMPLATE, dateRange);

        if (to.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter the recipient email address.",
                    "Recipient Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (cc.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter 2 CC email address.", "CC Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (selectedFile == null) {
            JOptionPane.showMessageDialog(this, "Please attach a file before sending the email.", "Attachment Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");

        Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(email, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(email));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));

            // Set CC recipients if provided
            if (!cc.isEmpty()) {
                message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(cc));
            }

            message.setSubject(subject);
            message.setText(body);

            // Set up the email with attachment
            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(body);
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);

            MimeBodyPart attachPart = new MimeBodyPart();
            attachPart.attachFile(selectedFile);
            multipart.addBodyPart(attachPart);

            message.setContent(multipart);
            Transport.send(message);
            JOptionPane.showMessageDialog(this, "Email sent successfully!");

        } catch (MessagingException | java.io.IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error while sending email: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            EmailSender emailSender = new EmailSender();
            emailSender.setVisible(true);
        });
    }
}
