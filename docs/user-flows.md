# Public Appointment Scheduling Flow

Route:

/schedule

Accessible without authentication.

## Step 1: Select Service Category

User selects one of:

* Brakes
* Oil Change
* Vehicle Inspection
* Tires
* Battery
* Engine
* Transmission
* Heating / Air Conditioning
* Scheduled Maintenance
* Other

If "Other" is selected:

* Show textarea
* User describes issue

Proceed to Step 2.

---

## Step 2: Select Detailed Service

Example:

Brakes →

* Brake Inspection
* Brake Pads Replacement
* Brake Rotors Replacement
* Brake Fluid Change
* Brake Noise Diagnosis
* Other

Proceed to Step 3.

---

## Step 3: Additional Information

User may:

* Add notes
* Upload images
* Upload videos

Attachments are optional.

Proceed to Step 4.

---

## Step 4: Phone Number

User enters mobile phone number.

Proceed to Step 5.

---

## Step 5: Phone Verification

System sends verification code.

User enters code.

Requirements:

* Code valid for 5 minutes
* Maximum 3 attempts

Proceed only if verification succeeds.

---

## Step 6: Customer & Vehicle Selection

Search customer by verified phone number.

If customer exists:

* Load vehicles

User can:

* Select existing vehicle
* Add new vehicle

Vehicle fields:

* VIN
* License Plate
* Brand
* Model
* Year

If customer does not exist:

* Create customer
* Create vehicle

Proceed to Step 7.

---

## Step 7: Visit Type

Choose:

* I will wait while the service is performed
* I will drop off my vehicle

Proceed to Step 8.

---

## Step 8: Appointment Scheduling

System displays:

* Working hours
* Available slots
* Holidays
* Existing bookings

User selects:

* Date
* Time Slot

System creates appointment.

Proceed to Step 9.

---

## Step 9: Confirmation

System:

* Creates appointment
* Sends confirmation SMS/email
* Sends cancellation link

Show success screen.
