# Software Lifecycle Management

# Mini Specification: Automotive Service Management and Vehicle Service History System

## Introduction

An increasing number of businesses are adopting digital solutions to improve operational efficiency, driven by the rapid advancement of information technologies and their integration into daily business activities.

Automotive service centers handle large amounts of information related to customers, vehicles, service interventions, appointments, and spare parts. Traditional record-keeping methods using paper documents, unstructured Word and Excel files, and phone-based communication often lead to misunderstandings, data loss, errors, and inefficient workflows.

The purpose of this system is to provide service center owners and mechanics with a centralized platform that automates and streamlines their daily operations related to customers and vehicles. The system will also improve communication between the service center and its customers.

---

# System Overview

The system will be a web-based application that allows service center staff to manage customers, vehicles, appointments, service records, and service history.

Mechanics and service staff will be able to create, update, and delete records while maintaining complete service histories for each vehicle.

The system will support searching for customers and vehicles using:

* License plate number
* VIN (Vehicle Identification Number)
* Vehicle owner

Service staff will be able to create and send service quotations to customers via email. Each quotation will clearly display:

* Spare part names
* Individual spare part prices
* Spare parts costs
* Labor costs
* Total service cost

Customers will be able to book service appointments by selecting available time slots that best suit their schedules.

A key feature of the system is maintaining detailed service histories for vehicles, including:

* Service type
* Service date
* Mileage
* Replaced parts
* Additional notes

After a service is completed, the system will automatically generate a service report containing all details of the performed work. This report will serve as an official service confirmation document.

Additionally, the system will analyze customer service history to identify loyal customers and automatically calculate discounts or loyalty rewards to improve customer satisfaction and retention.

---

# User Roles

The system will support the following user roles:

## Administrator

Administrators will have full access to the system, including:

* Managing employee accounts
* Managing system settings
* Managing user permissions and roles

## Service Staff / Mechanics

Mechanics and service employees will use the system to:

* Manage customers
* Manage vehicles
* Manage appointments
* Record completed services
* Create quotations
* Generate service documentation

## Customers

Customers will interact with the system by:

* Booking service appointments
* Canceling appointments
* Receiving appointment reminders
* Receiving quotations via email
* Receiving service reports and documentation
* Viewing notifications related to their vehicle services

---

# Functional Requirements

## User Management

The system shall provide:

* User login and authentication
* Administrator account management
* Creation and deletion of employee accounts
* Role-based access control (RBAC)
* Restricted access to features based on user roles

---

## Customer Management

The system shall provide:

* Add customer records
* Edit customer information
* Delete customer records
* Search customers by first name and last name
* View complete service history for each customer

---

## Appointment Management

The system shall provide:

* Display available service appointments
* Book service appointments
* Cancel service appointments
* Notify customers after appointment booking
* Send appointment reminder notifications before scheduled service
* Calendar view of all appointments for mechanics and staff

---

## Vehicle Management

The system shall provide:

* Add vehicle information
* Update vehicle information
* Search vehicles by:

    * License plate
    * VIN number
    * Vehicle owner
* Link vehicles to customers
* Create service records for vehicles

---

## Service and Service History Management

The system shall provide:

* Recording completed services including:

    * Minor service
    * Major service
    * Part replacement service
    * Custom service types
* Storing:

    * Service date
    * Vehicle mileage
    * Replaced parts
    * Service notes
* Viewing complete service history for each vehicle
* Quick access to previous services for future planning and diagnostics

---

## Quotation and Service Document Management

The system shall provide:

* Creation of service quotations
* Email delivery of quotations to customers
* Clear display of:

    * Spare parts costs
    * Labor costs
    * Total service cost
* Automatic generation of service documents after service completion
* Storage of generated documents within the system
* Email delivery of service documents to customers

---

## Loyalty and Discount Management

The system shall provide:

* Identification of repeat customers based on service history
* Tracking customer visit frequency
* Automatic discount calculation
* Loyalty rewards for returning customers
* Customer retention support

---

# Non-Functional Requirements

The system shall:

* Be intuitive and easy to use
* Provide secure storage of all customer and vehicle data
* Protect data from unauthorized access
* Maintain high performance with large numbers of customers and service records
* Ensure system stability during daily operation
* Minimize application errors and downtime
* Support future expansion and feature enhancements
* Be scalable to accommodate growing business needs

---

# Architecture, Technologies, and Tools

The application will be developed using a client-server architecture.

### Frontend

* React.js
* Responsive web interface

### Backend

* Java
* Spring Boot
* REST API architecture

### Database

* PostgreSQL

### Deployment

* Docker
* Kubernetes

---

# Conclusion

The Automotive Service Management and Vehicle Service History System aims to solve common operational challenges faced by automotive service centers and their customers.

The system will modernize and optimize daily service center operations by providing efficient management of customers, vehicles, appointments, services, quotations, and documentation.

The expected outcome is a fully functional platform that improves operational efficiency, enhances customer satisfaction, and provides long-term value for both service centers and their customers.
