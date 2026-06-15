export type Role = 'ROLE_ADMIN' | 'ROLE_MANAGER' | 'ROLE_EMPLOYEE';

export interface User {
  id: string;
  email: string;
  fullName: string;
  enabled: boolean;
  failedLoginAttempts: number;
  roles: Role[];
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: User;
}

export interface DashboardSummary {
  customers: number;
  vehicles: number;
  appointments: number;
  serviceRecords: number;
  offers: number;
}

export interface Customer {
  id: string;
  name: string;
  phone: string;
  email?: string;
  loyaltyPoints: number;
  notes?: string;
}

export interface CustomerLoyaltyStatus {
  customerId: string;
  completedServices: number;
  requiredServices: number;
  loyal: boolean;
  discountPercent: number;
}

export interface Vehicle {
  id: string;
  customerId: string;
  customerName?: string;
  plate: string;
  make: string;
  model: string;
  year: number;
  vin?: string;
  fuelType?: string;
  engine?: string;
}

export interface Appointment {
  id: string;
  customerId: string;
  customerName?: string;
  vehicleId: string;
  vehiclePlate?: string;
  vehicleName?: string;
  title: string;
  startsAt: string;
  endsAt: string;
  status: 'SCHEDULED' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED';
  cancellationUrl?: string;
}

export interface AppointmentCancellationInfo {
  customerName: string;
  vehiclePlate: string;
  vehicleName: string;
  scheduledAt: string;
  endsAt: string;
  serviceType: string;
  status: Appointment['status'];
  cancellable: boolean;
  message: string;
}

export interface AppointmentSlot {
  startsAt: string;
  endsAt: string;
}

export interface ServiceRecord {
  id: string;
  customerId: string;
  customerName?: string;
  vehicleId: string;
  vehiclePlate?: string;
  vehicleName?: string;
  performedAt: string;
  mileage: number;
  summary: string;
  cost: number;
  partsCost: number;
  laborCost: number;
  replacedParts?: string;
  notes?: string;
}

export interface OfferPart {
  name: string;
  price: number;
}

export interface Offer {
  id: string;
  customerId: string;
  vehicleId?: string;
  title: string;
  parts: OfferPart[];
  subtotal: number;
  discountPercent: number;
  discountAmount: number;
  total: number;
  partsCost: number;
  laborCost: number;
  status: 'DRAFT' | 'SENT' | 'ACCEPTED' | 'DECLINED';
}

export interface DocumentRecord {
  id: string;
  customerId?: string;
  serviceRecordId?: string;
  title: string;
  type: string;
  contentType?: string;
  storageKey?: string;
  uploadedAt: string;
}

export interface LoyaltyRule {
  id: string;
  name: string;
  pointsPerDenar: number;
  active: boolean;
}
