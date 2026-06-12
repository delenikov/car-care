# Software Requirements Specification (SRS)

# Automotive Service Management and Vehicle Service History System

## 1.4 Product Scope

The Automotive Service Management and Vehicle Service History System is a web-based application designed to digitize and optimize operational processes within an automotive service center.

The primary objective of the system is to provide centralized, structured, and secure management of data related to employees, customers, vehicles, appointments, and service operations. The system aims to reduce human error, prevent data loss, and improve communication efficiency.

The system shall support:

* Employee Management
* Customer Management
* Vehicle Management
* Service Management
* Appointment Management
* Quotation Management
* Service Document Management
* Loyalty and Discount Management

The initial version of the system is intended for a single automotive service center.

The system shall not process electronic payments, integrate with payment processors, or store payment card information. All payments for completed services are performed physically at the service center and are outside the scope of this version.

---

## 1.5 References

This document is based on internationally recognized standards and best practices in software requirements engineering.

References:

* IEEE 830 – Software Requirements Specification
* IEEE 29148 – Requirements Engineering Standard

---

# 2. General Description

## 2.1 Product Perspective

The system is a standalone web application based on a client-server architecture.

It is intended to replace traditional paper-based records and unstructured digital records (Word and Excel files) with centralized, structured, and secure data management.

The application operates independently and is not integrated with external ERP, accounting, financial, or payment systems.

Version 1 supports a single service center.

---

## 2.2 Product Functions

The system provides integrated management of all key processes within an automotive service center.

Main functionalities include:

* User authentication and authorization
* Customer and vehicle management
* Service history tracking
* Appointment scheduling
* Customer communication
* Loyalty and discount calculation

More specifically, the system shall provide:

### Authentication and Authorization

* Role-based access control (RBAC)
* User authentication
* Permission management based on user roles

### Customer and Vehicle Management

* Create, update, delete, and search customer records
* Create, update, delete, and search vehicle records
* Associate vehicles with customers

### Service History Management

Store and display:

* Service type
* Service date
* Vehicle mileage
* Replaced parts
* Financial information

### Appointment Management

* Display available appointments
* Appointment scheduling
* Appointment conflict prevention
* Appointment cancellation via secure time-limited links

### Customer Communication

Automatic email notifications for:

* Quotations
* Appointment confirmations
* Appointment reminders
* Service documents

### Loyalty Program

* Identification of loyal customers
* Discount calculation based on service history and visit frequency

These functionalities provide complete digitalization of service center operations and improve organization, efficiency, and customer communication.

---

## 2.3 User Classes and Characteristics

The system supports multiple user categories with different responsibilities and permission levels.

### 2.3.1 Administrator

The Administrator has the highest level of privileges.

Responsibilities include:

* Managing employee accounts
* Managing permissions and access control
* Maintaining system configuration

### 2.3.2 Service Employee / Mechanic

The Service Employee is the primary active user of the system.

Responsibilities include:

* Managing customers
* Managing vehicles
* Recording services
* Managing appointments
* Creating quotations
* Generating service documentation

### 2.3.3 Customer (External Actor)

Customers do not have direct access through user accounts.

They interact indirectly through:

* Service appointment requests
* Email confirmations
* Service reports
* Notifications and reminders

---

## 2.4 Operating Environment

The application is web-based and accessible through an internet connection and a standard web browser.

No local software installation is required.

Supported browsers:

* Google Chrome
* Mozilla Firefox
* Microsoft Edge
* Safari

Server requirements:

* Java runtime version compatible with Spring Boot

---

## 2.5 Design Constraints

The system shall follow a client-server architecture using REST communication.

Required technologies:

### Frontend

* React

### Backend

* Spring Boot (Java)

### Database

* PostgreSQL

---

## 2.6 User Documentation

The system shall provide electronic documentation in PDF format.

Documentation shall include:

### Administrator Guide

* User account management
* Access control management
* System administration features

### Service Employee Guide

* Customer management
* Vehicle management
* Service management
* Appointment management
* Quotation management
* Document management

---

## 2.7 Assumptions and Dependencies

### Internet Connectivity

Users are expected to have stable internet access because the system is web-based.

### Email Server Availability

Sending quotations, confirmations, reminders, and service documents depends on the availability and proper configuration of an SMTP email server.

### User Technical Skills

Administrators and service employees are expected to possess basic computer and web application skills.

---

# 3. External Interface Requirements

## 3.1 User Interface Requirements

### EIR-1

The system shall provide a web interface developed using React.

### EIR-2

The interface shall provide a calendar view for appointment management.

### EIR-3

The interface shall provide forms for data entry, editing, and viewing.

### EIR-4

All forms shall validate required fields.

### EIR-5

The interface shall support responsive design for different screen sizes.

---

## 3.2 Hardware Interface Requirements

### EIR-6

The system shall not require specialized hardware.

### EIR-7

Users shall be able to access the system using a computer with internet connectivity.

---

## 3.3 Software Interface Requirements

### EIR-8

The system shall communicate with a PostgreSQL database.

### EIR-9

The system shall communicate with an SMTP email server.

### EIR-10

The backend shall expose a REST API.

---

# 4. System Features and Functional Requirements

## 4.1 System User Management

* FR-1: The system shall allow Administrators and Service Employees to log in using a username and password.
* FR-2: The system shall validate credentials before granting access.
* FR-3: The system shall provide secure logout functionality.
* FR-4: The system shall allow users to change their password.
* FR-5: Administrators shall be able to create Service Employee accounts.
* FR-6: Administrators shall be able to update Service Employee accounts.
* FR-7: Administrators shall be able to delete Service Employee accounts.
* FR-8: The system shall implement role-based access control.

---

## 4.2 Customer Management

* FR-9: Add customers.
* FR-10: Update customer information.
* FR-11: Delete customers.
* FR-12: Store customer information including:

  * First name
  * Last name
  * Email
  * Phone number
  * Address
* FR-13: Search customers by first name.
* FR-14: Search customers by last name.
* FR-15: Display customer vehicles.
* FR-16: Display customer service history.

---

## 4.3 Vehicle Management

* FR-17: Add vehicles.
* FR-18: Update vehicle information.
* FR-19: Store vehicle information including:

  * VIN
  * License Plate Number
  * Brand
  * Model
  * Year
* FR-20: Search vehicles by VIN.
* FR-21: Search vehicles by License Plate Number.
* FR-22: Search vehicles by Owner.
* FR-23: Associate vehicles with customers.

---

## 4.4 Service and Service History Management

* FR-24: Service employees shall be able to record vehicle services.
* FR-25: The system shall support:

  * Minor Service
  * Major Service
  * Part Replacement Service
  * Custom Service Types
* FR-26: Store:

  * Service Date
  * Mileage
  * Parts Cost
  * Labor Cost
* FR-27: Automatically calculate total service cost.
* FR-28: Display complete vehicle service history.

---

## 4.5 Appointment Management

* FR-29: Display available appointments for a selected date.
* FR-30: Schedule appointments.
* FR-31: Prevent appointment conflicts.
* FR-32: Generate a unique secure appointment cancellation link valid for 24 hours.
* FR-33: Disable cancellation links after expiration or successful use.
* FR-34: Send appointment confirmation emails.
* FR-35: Send appointment reminder emails.
* FR-36: Display scheduled appointments.

---

## 4.6 Quotations and Service Documents

* FR-37: Service employees shall be able to create quotations.
* FR-38: Display a detailed cost breakdown including:

  * Parts Cost
  * Labor Cost
  * Total Cost
* FR-39: Send quotations via email.
* FR-40: Automatically generate service documents after service completion.
* FR-41: Send generated service documents to customers.
* FR-42: Support PDF export.

---

## 4.7 Loyalty and Discount Management

* FR-43: Identify loyal customers based on completed services.
* FR-44: Calculate discounts for loyal customers in future quotations.

---

# 5. Non-Functional Requirements

## 5.1 Performance

* NFR-1: System response time shall not exceed 5 seconds under normal load.
* NFR-2: The system shall support at least 50 concurrent active users.
* NFR-3: Service document and quotation generation shall not exceed 5 seconds.

---

## 5.2 Security

* NFR-4: Passwords shall be stored using bcrypt hashing.
* NFR-5: JWT shall be used for authentication.
* NFR-6: All client-server communication shall use HTTPS.
* NFR-7: The system shall implement role-based access control.
* NFR-8: The system shall prevent SQL Injection and Cross-Site Scripting (XSS) attacks through input validation.
* NFR-9: Failed login attempts shall be logged.

---

## 5.3 Reliability and Availability

* NFR-10: The system shall maintain at least 99% monthly uptime.
* NFR-11: Database backups shall be performed at least once daily.
* NFR-12: Backup restoration shall be possible within 24 hours.

---

## 5.4 Usability

* NFR-13: The system shall provide an intuitive user interface.
* NFR-14: Frequently used functions shall be accessible within a small number of clicks.
* NFR-15: The system shall provide clear navigation and structured menus.
* NFR-16: The system shall display informative success messages and clear error messages.
* NFR-17: The system shall support the Macedonian language.

---

## 5.5 Maintainability and Scalability

* NFR-18: The system shall use a modular architecture.
* NFR-19: Source code shall be well-structured and documented.
* NFR-20: The system shall support easy integration of new features without major redesign.

---

# 6. Future Enhancements

The following features are outside the scope of the current version but may be implemented in future releases:

* FE-1: SMS notifications for customers.
* FE-2: Progressive Web Application (PWA) / Mobile App support.
* FE-3: Integration with invoicing and fiscal receipt systems.
* FE-4: Support for multiple service centers within a single platform.
