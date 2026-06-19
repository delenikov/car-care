import { expect, test, type Page } from '@playwright/test';

const employee = {
  id: '2',
  email: 'tech@carcare.test',
  fullName: 'Integration Technician',
  enabled: true,
  failedLoginAttempts: 0,
  roles: ['ROLE_EMPLOYEE']
};

const admin = {
  id: '1',
  email: 'admin@carcare.test',
  fullName: 'Admin User',
  enabled: true,
  failedLoginAttempts: 0,
  roles: ['ROLE_ADMIN']
};

const ok = (data: unknown, message = 'OK') => ({
  success: true,
  message,
  data
});

const fail = (message: string) => ({
  success: false,
  message,
  data: null
});

async function mockBackend(page: Page) {
  await page.route('**/api/auth/login', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({ accessToken: 'access-token', refreshToken: 'refresh-token', user: admin }))
    });
  });

  await page.route('**/api/auth/logout', async (route) => {
    await route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok(null, 'Logged out')) });
  });

  await page.route('**/api/auth/change-password', async (route) => {
    await route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok(null, 'Password changed')) });
  });

  await page.route('**/api/dashboard/summary', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({ customers: 2, vehicles: 1, appointments: 1, serviceRecords: 1, offers: 1 }))
    });
  });

  await page.route('**/api/customers', async (route) => {
    if (route.request().method() === 'POST') {
      const body = route.request().postDataJSON();
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(ok({ id: '999', ...body }))
      });
      return;
    }
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok([{ id: '101', firstName: 'Ada', lastName: 'Lovelace', fullName: 'Ada Lovelace', email: 'ada@carcare.test', phone: '+38970111111', address: 'Analytical Lane' }]))
    });
  });

  await page.route('**/api/customers/*', async (route) => {
    const request = route.request();
    const pathname = new URL(request.url()).pathname;
    if (pathname.endsWith('/vehicles')) {
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(ok([{ id: '201', customerId: '101', customerName: 'Ada Lovelace', plateNumber: 'SK-1234-AA', make: 'Volkswagen', model: 'Golf', modelYear: 2020, vin: 'VIN123', fuelType: 'Diesel', engine: '2.0 TDI' }]))
      });
      return;
    }
    if (pathname.endsWith('/service-history')) {
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(ok([{ id: '401', customerId: '101', vehicleId: '201', serviceDate: '2026-06-12', serviceType: 'Oil and filters', totalAmount: 3500, odometer: 123456 }]))
      });
      return;
    }
    if (pathname.endsWith('/loyalty-status')) {
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(ok({ customerId: '101', completedServices: 4, requiredServices: 5, loyal: false, discountPercent: 0 }))
      });
      return;
    }
    if (request.method() === 'DELETE') {
      await route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok(null, 'Deleted')) });
      return;
    }
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({ id: '101', firstName: 'Ada', lastName: 'Lovelace', fullName: 'Ada Lovelace', email: 'ada@carcare.test', phone: '+38970111111', address: 'Analytical Lane' }))
    });
  });

  await page.route('**/api/vehicles', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok([{ id: '201', customerId: '101', customerName: 'Ada Lovelace', plateNumber: 'SK-1234-AA', make: 'Volkswagen', model: 'Golf', modelYear: 2020, vin: 'VIN123', fuelType: 'Diesel', engine: '2.0 TDI' }]))
    });
  });

  await page.route('**/api/vehicles/*', async (route) => {
    const pathname = new URL(route.request().url()).pathname;
    if (pathname.endsWith('/service-history')) {
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(ok([{ id: '401', customerId: '101', vehicleId: '201', serviceDate: '2026-06-12', serviceType: 'Oil and filters', partsCost: 1500, laborCost: 2000, totalAmount: 3500, odometer: 123456, replacedParts: 'Oil filter' }]))
      });
      return;
    }
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({ id: '201', customerId: '101', customerName: 'Ada Lovelace', plateNumber: 'SK-1234-AA', make: 'Volkswagen', model: 'Golf', modelYear: 2020, vin: 'VIN123', fuelType: 'Diesel', engine: '2.0 TDI' }))
    });
  });

  await page.route('**/api/appointments', async (route) => {
    if (route.request().method() === 'POST') {
      const body = route.request().postDataJSON();
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(ok({ id: '302', customerId: body.customerId, vehicleId: body.vehicleId, scheduledAt: body.startsAt, endsAt: body.endsAt, serviceType: body.title, status: 'SCHEDULED', cancellationUrl: 'http://localhost:5173/reservations/cancel/token-302' }))
      });
      return;
    }
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok([{ id: '301', customerId: '101', vehicleId: '201', scheduledAt: '2026-06-15T09:00:00.000Z', endsAt: '2026-06-15T10:00:00.000Z', serviceType: 'Oil change', status: 'SCHEDULED', cancellationUrl: 'http://localhost:5173/reservations/cancel/token-301' }]))
    });
  });

  await page.route('**/api/appointments/available**', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok([{ startsAt: '2026-06-20T08:00:00.000+02:00', endsAt: '2026-06-20T09:00:00.000+02:00' }]))
    });
  });

  await page.route('**/api/appointments/reminders**', async (route) => {
    await route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok({ sent: 1 })) });
  });

  await page.route('**/api/service-records', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok([{ id: '401', customerId: '101', vehicleId: '201', serviceDate: '2026-06-12', serviceType: 'Oil and filters', partsCost: 1500, laborCost: 2000, totalAmount: 3500, odometer: 123456, replacedParts: 'Oil filter' }]))
    });
  });

  await page.route('**/api/service-records/*', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({ id: '401', customerId: '101', vehicleId: '201', serviceDate: '2026-06-12', serviceType: 'Oil and filters', partsCost: 1500, laborCost: 2000, totalAmount: 3500, odometer: 123456, replacedParts: 'Oil filter', notes: 'Oil and filters' }))
    });
  });

  await page.route('**/api/offers', async (route) => {
    if (route.request().method() === 'POST') {
      const body = route.request().postDataJSON();
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(ok({ id: '502', customerName: 'Ada Lovelace', vehiclePlate: 'SK-1234-AA', vehicleName: 'Volkswagen Golf', ...body, amount: Number(body.partsCost) + Number(body.laborCost), status: 'PENDING_DELIVERY' }))
      });
      return;
    }
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok([{ id: '501', customerId: '101', customerName: 'Ada Lovelace', vehicleId: '201', vehiclePlate: 'SK-1234-AA', vehicleName: 'Volkswagen Golf', title: 'Brake inspection', partsCost: 700, laborCost: 500, amount: 1200, status: 'PENDING_DELIVERY' }]))
    });
  });

  await page.route('**/api/offers/**', async (route) => {
    const request = route.request();
    const pathname = new URL(request.url()).pathname;
    if (pathname.endsWith('/pdf')) {
      await route.fulfill({ contentType: 'application/pdf', body: '%PDF-1.4 quote' });
      return;
    }
    if (pathname.endsWith('/send')) {
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(ok({ id: '502', customerId: '101', customerName: 'Ada Lovelace', vehicleId: '201', vehiclePlate: 'SK-1234-AA', vehicleName: 'Volkswagen Golf', title: 'Brake inspection', partsCost: 700, laborCost: 500, amount: 1200, status: 'SENT' }))
      });
      return;
    }
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({ id: '502', customerId: '101', customerName: 'Ada Lovelace', vehicleId: '201', vehiclePlate: 'SK-1234-AA', vehicleName: 'Volkswagen Golf', title: 'Brake inspection', partsCost: 700, laborCost: 500, amount: 1200, status: 'PENDING_DELIVERY' }))
    });
  });

  await page.route('**/api/documents', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok([{ id: '601', customerId: '101', serviceRecordId: '401', type: 'INSPECTION', fileName: 'Inspection report.pdf', contentType: 'application/pdf', storageKey: 'generated/inspection-report.pdf' }]))
    });
  });

  await page.route('**/api/documents/**', async (route) => {
    const pathname = new URL(route.request().url()).pathname;
    if (pathname.endsWith('/pdf')) {
      await route.fulfill({ contentType: 'application/pdf', body: '%PDF-1.4 document' });
      return;
    }
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({ id: '601', customerId: '101', serviceRecordId: '401', type: 'INSPECTION', fileName: 'Inspection report.pdf', contentType: 'application/pdf', storageKey: 'generated/inspection-report.pdf' }))
    });
  });

  await page.route('**/api/admin/users', async (route) => {
    await route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok([admin, employee])) });
  });

}

async function signIn(page: Page, user = admin) {
  await page.addInitScript((storedUser) => {
    window.sessionStorage.setItem('carcare.accessToken', 'access-token');
    window.sessionStorage.setItem('carcare.refreshToken', 'refresh-token');
    window.sessionStorage.setItem('carcare.user', JSON.stringify(storedUser));
  }, user);
}

async function fillDatePickerGroup(page: Page, name: string, value: string) {
  const [day, month, year] = value.split('.');
  const group = page.getByRole('group', { name });
  await group.getByRole('spinbutton', { name: 'Day' }).fill(day);
  await group.getByRole('spinbutton', { name: 'Month' }).fill(month);
  await group.getByRole('spinbutton', { name: 'Year' }).fill(year);
  await page.keyboard.press('Tab');
}

async function fillTimePickerGroup(page: Page, name: string, value: string) {
  const [hours, minutes] = value.split(':');
  const group = page.getByRole('group', { name });
  await group.getByRole('spinbutton', { name: 'Hours' }).fill(hours);
  await group.getByRole('spinbutton', { name: 'Minutes' }).fill(minutes);
  await page.keyboard.press('Tab');
}

test.beforeEach(async ({ page }) => {
  await mockBackend(page);
});

test('redirects unauthenticated users to login', async ({ page }) => {
  await page.goto('/');

  await expect(page).toHaveURL(/\/login$/);
  await expect(page.locator('input[name="email"]')).toBeVisible();
  await expect(page.locator('input[name="password"]')).toBeVisible();
});

test('logs in and opens the dashboard summary', async ({ page }) => {
  await page.goto('/login');

  await page.locator('input[name="email"]').fill('admin@carcare.test');
  await page.locator('input[name="password"]').fill('password123');
  await page.locator('button[type="submit"]').click();

  await expect(page).toHaveURL('http://127.0.0.1:5173/');
  await expect(page.getByTestId('app-toast')).toHaveCount(0);
  await expect(page.getByTestId('dashboard-hero-card')).toHaveCSS('background-image', 'none');
  await expect(page.getByTestId('dashboard-hero-card')).toHaveCSS('background-color', 'rgb(20, 35, 31)');
  await expect(page.getByText('Следете клиенти, возила, термини, сервисни записи и понуди од едно работно место.')).toBeVisible();
  await expect(page.getByRole('main').getByText('Клиенти', { exact: true })).toBeVisible();
  await expect(page.getByRole('main').getByText('Возила', { exact: true })).toBeVisible();
});

test('shows specific login validation errors for missing email and wrong password', async ({ page }) => {
  await page.unroute('**/api/auth/login');
  await page.route('**/api/auth/login', async (route) => {
    const body = route.request().postDataJSON() as { email: string; password: string };
    if (body.email !== admin.email) {
      await route.fulfill({ status: 401, contentType: 'application/json', body: JSON.stringify(fail('User with that email does not exist')) });
      return;
    }
    if (body.password !== 'password123') {
      await route.fulfill({ status: 401, contentType: 'application/json', body: JSON.stringify(fail('Password is wrong')) });
      return;
    }
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({ accessToken: 'access-token', refreshToken: 'refresh-token', user: admin }))
    });
  });

  await page.goto('/login');
  await page.locator('input[name="email"]').fill('missing@carcare.test');
  await page.locator('input[name="password"]').fill('password123');
  await page.locator('button[type="submit"]').click();

  await expect(page.getByRole('alert')).toContainText('User with that email does not exist');

  await page.locator('input[name="email"]').fill(admin.email);
  await page.locator('input[name="password"]').fill('wrong-password');
  await page.locator('button[type="submit"]').click();

  await expect(page.getByRole('alert')).toContainText('Password is wrong');
});

test('shows implemented authenticated module pages', async ({ page }) => {
  await signIn(page);

  const textChecks: Array<[string, string]> = [
    ['/customers', 'Ada Lovelace'],
    ['/vehicles', 'Volkswagen Golf'],
    ['/services', 'Oil and filters'],
    ['/offers', 'Brake inspection'],
    ['/documents', 'Inspection report'],
    ['/admin', 'Integration Technician']
  ];

  for (const [path, expectedText] of textChecks) {
    await page.goto(path);
    await expect(page.getByText(expectedText).first()).toBeVisible();
  }

  await page.goto('/appointments');
  await expect(page.locator('.fc')).toBeVisible();
});

test('hides and blocks admin page for non-admin users', async ({ page }) => {
  let adminUsersRequested = false;
  await page.route('**/api/admin/users**', async (route) => {
    adminUsersRequested = true;
    await route.fulfill({ status: 403, contentType: 'application/json', body: JSON.stringify(fail('Forbidden')) });
  });
  await signIn(page, employee);

  await page.goto('/');
  await expect(page.locator('a[href="/admin"]')).toHaveCount(0);

  await page.goto('/admin');
  await expect(page).toHaveURL('http://127.0.0.1:5173/');
  await expect(page.locator('a[href="/admin"]')).toHaveCount(0);
  expect(adminUsersRequested).toBe(false);
});

test('creates a customer with the backend DTO shape expected by the API client', async ({ page }) => {
  await signIn(page);
  let customerPayload: Record<string, unknown> | undefined;

  await page.route('**/api/customers', async (route) => {
    if (route.request().method() === 'POST') {
      customerPayload = route.request().postDataJSON();
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(ok({ id: '999', ...customerPayload }))
      });
      return;
    }
    await route.fallback();
  });

  await page.goto('/customers/new');
  await page.locator('input[name="name"]').fill('Grace Hopper');
  await page.locator('input[name="phone"]').fill('+38970222222');
  await page.locator('input[name="email"]').fill('grace@carcare.test');
  await page.locator('textarea[name="notes"]').fill('Compiler Street');
  await page.locator('button[type="submit"]').click();

  await expect(page).toHaveURL(/\/customers\/999$/);
  expect(customerPayload).toEqual({
    firstName: 'Grace',
    lastName: 'Hopper',
    fullName: 'Grace Hopper',
    email: 'grace@carcare.test',
    phone: '+38970222222',
    address: 'Compiler Street'
  });
});

test('searches customers and shows customer vehicles and service history', async ({ page }) => {
  await signIn(page);
  let searchUrl = '';
  let deletedCustomerId = '';

  await page.route('**/api/customers**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    if (url.pathname === '/api/customers' && request.method() === 'GET') {
      searchUrl = url.toString();
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(ok([{ id: '101', firstName: 'Ada', lastName: 'Lovelace', fullName: 'Ada Lovelace', email: 'ada@carcare.test', phone: '+38970111111', address: 'Analytical Lane' }]))
      });
      return;
    }
    if (url.pathname.endsWith('/vehicles')) {
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(ok([{ id: '201', customerId: '101', customerName: 'Ada Lovelace', plateNumber: 'SK-1234-AA', make: 'Volkswagen', model: 'Golf', modelYear: 2020, vin: 'VIN123', fuelType: 'Diesel', engine: '2.0 TDI' }]))
      });
      return;
    }
    if (url.pathname.endsWith('/service-history')) {
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(ok([{ id: '401', customerId: '101', vehicleId: '201', serviceDate: '2026-06-12', serviceType: 'Oil and filters', totalAmount: 3500, odometer: 123456 }]))
      });
      return;
    }
    if (request.method() === 'DELETE') {
      deletedCustomerId = url.pathname.split('/').pop() ?? '';
      await route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok(null, 'Deleted')) });
      return;
    }
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({ id: '101', firstName: 'Ada', lastName: 'Lovelace', fullName: 'Ada Lovelace', email: 'ada@carcare.test', phone: '+38970111111', address: 'Analytical Lane' }))
    });
  });

  await page.goto('/customers');
  await page.locator('input[name="firstName"]').fill('Ada');
  await page.locator('input[name="lastName"]').fill('Lovelace');
  await page.getByRole('button', { name: 'Пребарај' }).click();

  await expect(page.getByText('Ada Lovelace').first()).toBeVisible();
  expect(searchUrl).toContain('firstName=Ada');
  expect(searchUrl).toContain('lastName=Lovelace');

  await page.getByRole('link', { name: 'Детали' }).click();
  await expect(page.getByText('Возила на клиентот')).toBeVisible();
  await expect(page.getByText('SK-1234-AA')).toBeVisible();
  await expect(page.locator('a[href="/services/new?customerId=101&vehicleId=201"]')).toBeVisible();
  await expect(page.getByText('Сервисна историја')).toBeVisible();
  await expect(page.getByText('Oil and filters')).toBeVisible();

  await page.getByRole('button', { name: 'Избриши' }).click();
  await expect(page).toHaveURL(/\/customers$/);
  expect(deletedCustomerId).toBe('101');
});

test('searches creates and updates vehicles with backend DTO mapping', async ({ page }) => {
  await signIn(page);
  const backendVehicles = [
    { id: '201', customerId: '101', customerName: 'Ada Lovelace', plateNumber: 'SK-1234-AA', make: 'Volkswagen', model: 'Golf', modelYear: 2020, vin: '5B4LP57F1X3453221', fuelType: 'Diesel', engine: '2.0 TDI' }
  ];
  let searchUrl = '';
  let createPayload: Record<string, unknown> | undefined;
  let updatePayload: Record<string, unknown> | undefined;

  await page.route('**/api/vehicles**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    if (url.pathname === '/api/vehicles' && request.method() === 'GET') {
      searchUrl = url.toString();
      await route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok(backendVehicles)) });
      return;
    }
    if (url.pathname === '/api/vehicles' && request.method() === 'POST') {
      createPayload = request.postDataJSON();
      const created = { id: '202', ...createPayload };
      backendVehicles.push(created as (typeof backendVehicles)[number]);
      await route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok(created)) });
      return;
    }
    if (request.method() === 'PUT') {
      updatePayload = request.postDataJSON();
      const id = url.pathname.split('/').pop() ?? '';
      const index = backendVehicles.findIndex((vehicle) => vehicle.id === id);
      backendVehicles[index] = { id, ...updatePayload } as (typeof backendVehicles)[number];
      await route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok(backendVehicles[index])) });
      return;
    }
    if (url.pathname.endsWith('/service-history')) {
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(ok([{ id: '401', customerId: '101', vehicleId: '202', serviceDate: '2026-06-12', serviceType: 'Major Service', partsCost: 2500, laborCost: 1500, totalAmount: 4000, odometer: 123456, replacedParts: 'Air filter' }]))
      });
      return;
    }
    const id = url.pathname.split('/').pop() ?? '';
    const vehicle = backendVehicles.find((item) => item.id === id) ?? backendVehicles[0];
    await route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok(vehicle)) });
  });

  await page.goto('/vehicles');
  await page.locator('input[name="vehicleSearch"]').fill('SK-1234-AA');
  await page.getByRole('button', { name: 'Пребарај' }).click();

  await expect(page.getByText('Volkswagen Golf').first()).toBeVisible();
  await expect(page.getByText('5B4LP57F1X3453221').first()).toBeVisible();
  await expect(page.getByText('Ada Lovelace').first()).toBeVisible();
  await expect(page.getByText('Diesel').first()).toBeVisible();
  await expect(page.getByText('2.0 TDI').first()).toBeVisible();
  await expect(page.locator('mark').filter({ hasText: 'SK-1234-AA' })).toBeVisible();
  expect(searchUrl).toContain('q=SK-1234-AA');
  expect(searchUrl).not.toContain('vin=');
  expect(searchUrl).not.toContain('plateNumber=');
  expect(searchUrl).not.toContain('owner=');
  await page.getByLabel('Исчисти пребарување возила').click();
  await expect(page.locator('input[name="vehicleSearch"]')).toHaveValue('');
  await page.getByRole('button', { name: 'Ресетирај' }).click();
  await expect(page.locator('input[name="vehicleSearch"]')).toHaveValue('');

  await page.route('**/api/customers', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok([{ id: 101, firstName: 'Ada', lastName: 'Lovelace', fullName: 'Ada Lovelace', email: 'ada@carcare.test', phone: '+38970111111', address: 'Analytical Lane' }]))
    });
  });

  await page.goto('/vehicles/new');
  await page.getByLabel('Клиент').fill('Ada');
  await page.getByRole('option', { name: /Ada Lovelace/ }).click();
  await expect(page.getByLabel('Клиент')).toHaveValue('Ada Lovelace');
  await page.locator('input[name="plate"]').fill('OH-2020-AA');
  await page.locator('input[name="make"]').fill('Toyota');
  await page.locator('input[name="model"]').fill('Corolla');
  await page.locator('input[name="year"]').fill('2022');
  await page.locator('input[name="vin"]').fill('VIN202');
  await page.locator('input[name="fuelType"]').fill('Hybrid');
  await page.locator('input[name="engine"]').fill('1.8');
  await page.locator('button[type="submit"]').click();

  await expect(page).toHaveURL(/\/vehicles\/202$/);
  expect(createPayload).toEqual({
    customerId: '101',
    plateNumber: 'OH-2020-AA',
    make: 'Toyota',
    model: 'Corolla',
    modelYear: 2022,
    vin: 'VIN202',
    fuelType: 'Hybrid',
    engine: '1.8'
  });

  await page.goto('/vehicles/202/edit');
  await page.locator('input[name="plate"]').fill('OH-7777-AA');
  await page.locator('input[name="year"]').fill('2023');
  await page.locator('input[name="fuelType"]').fill('Petrol');
  await page.locator('input[name="engine"]').fill('2.0');
  await page.locator('button[type="submit"]').click();

  await expect(page).toHaveURL(/\/vehicles\/202$/);
  expect(updatePayload).toMatchObject({
    customerId: '101',
    plateNumber: 'OH-7777-AA',
    make: 'Toyota',
    model: 'Corolla',
    modelYear: 2023,
    vin: 'VIN202',
    fuelType: 'Petrol',
    engine: '2.0'
  });

  await page.locator('a[href="/services/new?customerId=101&vehicleId=202"]').click();
  await expect(page).toHaveURL(/\/services\/new\?customerId=101&vehicleId=202$/);
  await expect(page.getByLabel('Клиент')).toHaveValue('Ada Lovelace');
});

test('records services with parts labor and replaced parts', async ({ page }) => {
  await signIn(page);
  let servicePayload: Record<string, unknown> | undefined;

  await page.route('**/api/customers/*/vehicles', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok([{ id: '201', customerId: '101', customerName: 'Ada Lovelace', plateNumber: 'SK-1234-AA', make: 'Volkswagen', model: 'Golf', modelYear: 2020, vin: 'VIN123', fuelType: 'Diesel', engine: '2.0 TDI' }]))
    });
  });

  await page.route('**/api/service-records', async (route) => {
    const request = route.request();
    if (request.method() === 'POST') {
      servicePayload = request.postDataJSON();
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(ok({ id: '402', ...servicePayload, totalAmount: 3500 }))
      });
      return;
    }
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok([{ id: '401', customerId: '101', vehicleId: '201', serviceDate: '2026-06-12', serviceType: 'Oil and filters', partsCost: 1500, laborCost: 2000, totalAmount: 3500, odometer: 123456, replacedParts: 'Oil filter' }]))
    });
  });

  await page.goto('/services/new');
  await page.getByLabel('Клиент').fill('Ada');
  await page.getByRole('option', { name: 'Ada Lovelace', exact: true }).click();
  await expect(page.getByLabel('Клиент')).toHaveValue('Ada Lovelace');
  await expect(page.getByText(/Too small/i)).toHaveCount(0);
  await page.getByLabel('Возило').fill('SK-1234-AA');
  await page.getByRole('option', { name: /SK-1234-AA - Volkswagen Golf/ }).click();
  await expect(page.getByText(/Too small/i)).toHaveCount(0);
  await fillDatePickerGroup(page, 'Датум', '12.06.2026');
  await fillTimePickerGroup(page, 'Време', '14:35');
  await page.locator('input[name="mileage"]').fill('123456');
  await page.locator('input[name="summary"]').fill('Part Replacement Service');
  await page.getByRole('button', { name: 'Додај дел' }).click();
  await page.locator('input[name="parts.0.name"]').fill('Oil filter');
  await page.locator('input[name="parts.0.price"]').fill('1500');
  await page.locator('input[name="laborCost"]').fill('2000');
  await page.locator('textarea[name="notes"]').fill('Oil and filters');
  await expect(page.getByText(/Вкупно делови: 1[,.]500 ден\./)).toBeVisible();
  await page.locator('button[type="submit"]').click();

  await expect(page).toHaveURL(/\/services$/);
  expect(servicePayload).toEqual({
    customerId: '101',
    vehicleId: '201',
    serviceDate: '2026-06-12',
    serviceType: 'Part Replacement Service',
    partsCost: 1500,
    laborCost: 2000,
    odometer: 123456,
    replacedParts: 'Oil filter (1,500 ден.)',
    notes: 'Oil and filters'
  });
  expect(servicePayload).not.toHaveProperty('serviceTime');
});

test('opens service details from services and vehicle history', async ({ page }) => {
  await signIn(page);

  await page.route('**/api/vehicles/*/service-history', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok([{ id: '401', customerId: '101', vehicleId: '201', serviceDate: '2026-06-12', serviceType: 'Oil and filters', partsCost: 1500, laborCost: 2000, totalAmount: 3500, odometer: 123456, replacedParts: 'Oil filter' }]))
    });
  });

  await page.goto('/services');
  await page.getByRole('row', { name: /Oil and filters/ }).click();
  await expect(page).toHaveURL(/\/services\/401$/);
  await expect(page.getByRole('heading', { name: 'Oil and filters' })).toBeVisible();
  await expect(page.getByText('Заменети делови')).toBeVisible();
  await expect(page.getByText('Oil filter')).toBeVisible();
  await expect(page.getByText('3,500 ден.').first()).toBeVisible();

  await page.goto('/vehicles/201');
  await page.getByRole('row', { name: /Oil and filters/ }).click();
  await expect(page).toHaveURL(/\/services\/401$/);
  await expect(page.getByText('Oil and filters').first()).toBeVisible();
  await expect(page.getByText('123,456 km')).toBeVisible();
});

test('shows available appointments and schedules without conflicts', async ({ page }) => {
  await signIn(page);
  let appointmentPayload: Record<string, unknown> | undefined;
  let deletedAppointmentId = '';
  let availableUrl = '';
  let appointmentRows = [
    { id: 301, customerId: 101, customerName: 'Ada Lovelace', vehicleId: 201, vehiclePlate: 'SK-1234-AA', vehicleName: 'Volkswagen Golf', scheduledAt: '2026-06-15T09:00:00+02:00', endsAt: '2026-06-15T10:00:00+02:00', serviceType: 'Oil change', status: 'SCHEDULED' },
    { id: 304, customerId: 101, customerName: 'Ada Lovelace', vehicleId: 201, vehiclePlate: 'SK-1234-AA', vehicleName: 'Volkswagen Golf', scheduledAt: '2026-06-15T11:00:00+02:00', endsAt: '2026-06-15T12:00:00+02:00', serviceType: 'Cancelled check', status: 'CANCELLED' }
  ];

  await page.route('**/api/appointments/available**', async (route) => {
    availableUrl = route.request().url();
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok([{ startsAt: '2026-06-20T08:00:00.000+02:00', endsAt: '2026-06-20T09:00:00.000+02:00' }]))
    });
  });

  await page.route('**/api/customers/*/vehicles', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok([{ id: '201', customerId: '101', customerName: 'Ada Lovelace', plateNumber: 'SK-1234-AA', make: 'Volkswagen', model: 'Golf', modelYear: 2020, vin: 'VIN123', fuelType: 'Diesel', engine: '2.0 TDI' }]))
    });
  });

  await page.route('**/api/appointments', async (route) => {
    const request = route.request();
    if (request.method() === 'POST') {
      appointmentPayload = request.postDataJSON();
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(ok({ id: '302', customerId: '101', customerName: 'Ada Lovelace', vehicleId: '201', vehiclePlate: 'SK-1234-AA', vehicleName: 'Volkswagen Golf', scheduledAt: appointmentPayload?.startsAt, endsAt: appointmentPayload?.endsAt, serviceType: appointmentPayload?.title, status: 'SCHEDULED', cancellationUrl: 'http://localhost:5173/reservations/cancel/token-302' }))
      });
      return;
    }
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok(appointmentRows))
    });
  });

  await page.route('**/api/appointments/301', async (route) => {
    if (route.request().method() === 'DELETE') {
      deletedAppointmentId = '301';
      appointmentRows = appointmentRows.filter((appointment) => String(appointment.id) !== '301');
      await route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok(null, 'Appointment deleted')) });
      return;
    }
    await route.fallback();
  });

  await page.goto('/appointments');
  await expect(page.getByText('Oil change').first()).toBeVisible();
  await expect(page.getByText('Cancelled check')).toHaveCount(0);
  await expect(page.getByRole('group', { name: 'Провери достапност за датум' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Напредно / сопствено време' })).toBeVisible();
  await fillDatePickerGroup(page, 'Провери достапност за датум', '20.06.2026');
  const availableSlot = page.locator('.MuiChip-root').filter({ hasText: '08:00' });
  await expect(availableSlot).toBeVisible();
  expect(availableUrl).toContain('date=2026-06-20');

  await page.locator('button[type="submit"]').click();
  await expect(page.getByText('Полето е задолжително')).toHaveCount(2);
  await expect(page.getByText(/Too small/i)).toHaveCount(0);
  expect(appointmentPayload).toBeUndefined();

  await page.locator('input[name="customerId"]').fill('Ada');
  await page.getByRole('option', { name: 'Ada Lovelace', exact: true }).click();
  await page.locator('input[name="vehicleId"]').fill('SK-1234-AA');
  await page.getByRole('option', { name: /SK-1234-AA - Volkswagen Golf/ }).click();
  await page.locator('input[name="title"]').fill('Minor Service');
  await page.getByRole('button', { name: 'Напредно / сопствено време' }).click();
  await expect(page.getByRole('group', { name: 'Почеток датум' })).toBeVisible();
  await expect(page.getByRole('group', { name: 'Почеток време' })).toBeVisible();
  await expect(page.getByRole('group', { name: 'Крај датум' })).toBeVisible();
  await expect(page.getByRole('group', { name: 'Крај време' })).toBeVisible();

  await fillDatePickerGroup(page, 'Почеток датум', '20.06.2026');
  await fillTimePickerGroup(page, 'Почеток време', '17:00');
  await fillDatePickerGroup(page, 'Крај датум', '20.06.2026');
  await fillTimePickerGroup(page, 'Крај време', '18:00');
  await page.locator('button[type="submit"]').click();
  await expect(page.getByText('Терминот мора да биде во работно време од 08:00 до 16:00.').first()).toBeVisible();
  expect(appointmentPayload).toBeUndefined();

  await fillDatePickerGroup(page, 'Почеток датум', '15.06.2026');
  await fillTimePickerGroup(page, 'Почеток време', '09:30');
  await fillDatePickerGroup(page, 'Крај датум', '15.06.2026');
  await fillTimePickerGroup(page, 'Крај време', '10:30');
  await page.locator('button[type="submit"]').click();
  await expect(page.getByText('Терминот се преклопува со постоечки термин.').first()).toBeVisible();
  expect(appointmentPayload).toBeUndefined();

  await page.getByText('Oil change').first().click();
  const appointmentDialog = page.getByRole('dialog', { name: 'Oil change' });
  await expect(appointmentDialog).toBeVisible();
  await expect(appointmentDialog.getByText('Ada Lovelace')).toBeVisible();
  await expect(appointmentDialog.getByText('SK-1234-AA - Volkswagen Golf')).toBeVisible();
  await appointmentDialog.getByRole('button', { name: 'Избриши' }).click();
  await expect(page.getByRole('dialog', { name: 'Избриши термин' })).toBeVisible();
  await page.getByRole('dialog', { name: 'Избриши термин' }).getByRole('button', { name: 'Избриши' }).click();
  await expect(page.getByText('Oil change')).toHaveCount(0);
  expect(deletedAppointmentId).toBe('301');

  await availableSlot.click();
  await expect(page.locator('input[name="startsAtDate"]')).toHaveValue(/20\.06\.2026/);
  await expect(page.locator('input[name="startsAtTime"]')).toHaveValue(/08:00/);
  await expect(page.locator('input[name="endsAtDate"]')).toHaveValue(/20\.06\.2026/);
  await expect(page.locator('input[name="endsAtTime"]')).toHaveValue(/09:00/);
  await page.locator('button[type="submit"]').click();

  expect(appointmentPayload).toMatchObject({
    customerId: '101',
    vehicleId: '201',
    title: 'Minor Service'
  });
  expect(appointmentPayload?.startsAt).toBe('2026-06-20T08:00:00+02:00');
  expect(appointmentPayload?.endsAt).toBe('2026-06-20T09:00:00+02:00');
});

test('shows backend appointment conflict message when server rejects a save', async ({ page }) => {
  await signIn(page);
  let postCount = 0;

  await page.route('**/api/appointments/available**', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok([{ startsAt: '2026-06-20T08:00:00.000+02:00', endsAt: '2026-06-20T09:00:00.000+02:00' }]))
    });
  });

  await page.route('**/api/customers/*/vehicles', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok([{ id: '201', customerId: '101', customerName: 'Ada Lovelace', plateNumber: 'SK-1234-AA', make: 'Volkswagen', model: 'Golf', modelYear: 2020, vin: 'VIN123', fuelType: 'Diesel', engine: '2.0 TDI' }]))
    });
  });

  await page.route('**/api/appointments', async (route) => {
    if (route.request().method() === 'POST') {
      postCount += 1;
      await route.fulfill({ status: 409, contentType: 'application/json', body: JSON.stringify(fail('Appointment conflicts with an existing appointment')) });
      return;
    }
    await route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok([])) });
  });

  await page.goto('/appointments');
  await page.locator('.MuiChip-root').filter({ hasText: '08:00' }).click();
  await page.locator('input[name="customerId"]').fill('Ada');
  await page.getByRole('option', { name: 'Ada Lovelace', exact: true }).click();
  await page.locator('input[name="vehicleId"]').fill('SK-1234-AA');
  await page.getByRole('option', { name: /SK-1234-AA - Volkswagen Golf/ }).click();
  await page.locator('input[name="title"]').fill('Minor Service');
  await page.locator('button[type="submit"]').click();

  await expect(page.getByRole('alert')).toContainText('Appointment conflicts with an existing appointment');
  expect(postCount).toBe(1);
});

test('allows public customers to book an available appointment', async ({ page }) => {
  let publicPayload: Record<string, unknown> | undefined;

  await page.route('**/api/appointments/available**', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok([{ startsAt: '2026-06-20T08:00:00.000+02:00', endsAt: '2026-06-20T09:00:00.000+02:00' }]))
    });
  });

  await page.route('**/api/appointments/public', async (route) => {
    publicPayload = route.request().postDataJSON();
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({ id: '303', customerId: '101', customerName: publicPayload?.fullName, vehicleId: '201', vehiclePlate: publicPayload?.plateNumber, vehicleName: `${publicPayload?.make} ${publicPayload?.model}`, scheduledAt: publicPayload?.startsAt, endsAt: publicPayload?.endsAt, serviceType: publicPayload?.serviceType, status: 'SCHEDULED', cancellationUrl: 'http://localhost:5173/reservations/cancel/token-303' }))
    });
  });

  await page.goto('/book-appointment');
  await page.locator('.MuiChip-root').filter({ hasText: '08:00' }).click();
  await page.locator('input[name="fullName"]').fill('Public Customer');
  await page.locator('input[name="email"]').fill('public@carcare.test');
  await page.locator('input[name="phone"]').fill('+38970111111');
  await page.locator('input[name="serviceType"]').fill('Minor Service');
  await page.locator('input[name="plateNumber"]').fill('SK-9999-AA');
  await page.locator('input[name="vin"]').fill('WAUZZZ8V0KA000001');
  await page.locator('input[name="make"]').fill('Audi');
  await page.locator('input[name="model"]').fill('A3');
  await page.locator('input[name="modelYear"]').fill('2020');
  await page.locator('input[name="engine"]').fill('1.6 TDI');
  await page.locator('input[name="fuelType"]').fill('Diesel');
  await page.locator('button[type="submit"]').click();

  expect(publicPayload).toMatchObject({
    fullName: 'Public Customer',
    email: 'public@carcare.test',
    plateNumber: 'SK-9999-AA',
    vin: 'WAUZZZ8V0KA000001',
    make: 'Audi',
    model: 'A3',
    modelYear: 2020,
    engine: '1.6 TDI',
    fuelType: 'Diesel',
    startsAt: '2026-06-20T08:00:00+02:00',
    endsAt: '2026-06-20T09:00:00+02:00',
    serviceType: 'Minor Service'
  });
});

test('shows cancellation details before cancelling a public reservation', async ({ page }) => {
  let cancelledPayload: Record<string, unknown> | undefined;

  await page.route('**/api/appointments/cancel-info/token-303', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({
        customerName: 'Public Customer',
        vehiclePlate: 'SK-9999-AA',
        vehicleName: 'Audi A3',
        scheduledAt: '2026-06-20T08:00:00+02:00',
        endsAt: '2026-06-20T09:00:00+02:00',
        serviceType: 'Minor Service',
        status: 'SCHEDULED',
        cancellable: true,
        message: 'Терминот може да се откаже.'
      }))
    });
  });

  await page.route('**/api/appointments/cancel', async (route) => {
    cancelledPayload = route.request().postDataJSON();
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({ id: '303', customerId: '101', vehicleId: '201', scheduledAt: '2026-06-20T08:00:00+02:00', endsAt: '2026-06-20T09:00:00+02:00', serviceType: 'Minor Service', status: 'CANCELLED' }))
    });
  });

  await page.goto('/reservations/cancel/token-303');
  await expect(page.getByText('Public Customer')).toBeVisible();
  await expect(page.getByText('SK-9999-AA - Audi A3')).toBeVisible();
  await page.getByRole('button', { name: 'Откажи термин' }).click();

  expect(cancelledPayload).toEqual({ token: 'token-303' });
});

test('creates and sends quotations with a detailed cost breakdown', async ({ page }) => {
  await signIn(page);
  let offerPayload: Record<string, unknown> | undefined;
  let offerStatus = 'PENDING_DELIVERY';
  let offerParts: Array<{ name: string; price: number }> = [];
  let sendCalled = false;
  let quotePdfCalled = false;

  await page.route('**/api/customers/*/vehicles', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok([{ id: '201', customerId: '101', customerName: 'Ada Lovelace', plateNumber: 'SK-1234-AA', make: 'Volkswagen', model: 'Golf', modelYear: 2020, vin: 'VIN123', fuelType: 'Diesel', engine: '2.0 TDI' }]))
    });
  });

  await page.route('**/api/customers/*/loyalty-status', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({ customerId: '101', completedServices: 5, requiredServices: 5, loyal: true, discountPercent: 10 }))
    });
  });

  await page.route('**/api/offers', async (route) => {
    const request = route.request();
    if (request.method() === 'POST') {
      offerPayload = request.postDataJSON();
      offerStatus = 'SENT';
      offerParts = offerPayload.parts as Array<{ name: string; price: number }>;
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(ok({ id: '502', customerName: 'Ada Lovelace', vehiclePlate: 'SK-1234-AA', vehicleName: 'Volkswagen Golf', ...offerPayload, subtotalAmount: 1200, discountPercent: 10, discountAmount: 120, amount: 1080, status: offerStatus }))
      });
      return;
    }
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok([{ id: '501', customerId: '101', customerName: 'Ada Lovelace', vehicleId: '201', vehiclePlate: 'SK-1234-AA', vehicleName: 'Volkswagen Golf', title: 'Brake inspection', parts: [{ name: 'Brake pads', price: 700 }], partsCost: 700, laborCost: 500, subtotalAmount: 1200, discountPercent: 0, discountAmount: 0, amount: 1200, status: 'PENDING_DELIVERY' }]))
    });
  });

  await page.route('**/api/offers/**', async (route) => {
    const pathname = new URL(route.request().url()).pathname;
    if (pathname.endsWith('/pdf')) {
      quotePdfCalled = true;
      await route.fulfill({ contentType: 'application/pdf', body: '%PDF-1.4 quote' });
      return;
    }
    if (pathname.endsWith('/send')) {
      offerStatus = 'SENT';
      sendCalled = true;
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(ok({ id: '502', customerId: '101', customerName: 'Ada Lovelace', vehicleId: '201', vehiclePlate: 'SK-1234-AA', vehicleName: 'Volkswagen Golf', title: 'Brake inspection', parts: offerParts, partsCost: 700, laborCost: 500, subtotalAmount: 1200, discountPercent: 10, discountAmount: 120, amount: 1080, status: offerStatus }))
      });
      return;
    }
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({ id: '502', customerId: '101', customerName: 'Ada Lovelace', vehicleId: '201', vehiclePlate: 'SK-1234-AA', vehicleName: 'Volkswagen Golf', title: 'Brake inspection', parts: offerParts, partsCost: 700, laborCost: 500, subtotalAmount: 1200, discountPercent: 10, discountAmount: 120, amount: 1080, status: offerStatus }))
    });
  });

  await page.goto('/offers/new');
  await page.getByLabel('Клиент').fill('Ada');
  await page.getByRole('option', { name: 'Ada Lovelace', exact: true }).click();
  const vehiclePicker = page.getByRole('combobox', { name: 'Возило' });
  await expect(vehiclePicker).toBeEnabled();
  await vehiclePicker.click();
  await vehiclePicker.fill('SK-1234-AA');
  await page.getByRole('option', { name: /SK-1234-AA - Volkswagen Golf/ }).click();
  await page.locator('input[name="title"]').fill('Brake inspection');
  await page.getByRole('button', { name: 'Додај дел' }).click();
  await page.locator('input[name="parts.0.name"]').fill('Brake pads');
  await page.locator('input[name="parts.0.price"]').fill('700');
  await page.locator('input[name="laborCost"]').fill('500');
  await expect(page.getByText(/10%/)).toBeVisible();
  await expect(page.locator('input[value="-120 ден. (10%)"]')).toBeVisible();
  await expect(page.locator('input[value="1,080 ден."]')).toBeVisible();
  await expect(page.getByText(/Вкупно делови: 700 ден\./)).toBeVisible();
  await page.locator('button[type="submit"]').click();

  await expect(page).toHaveURL(/\/offers\/502$/);
  expect(offerPayload).toEqual({
    customerId: '101',
    vehicleId: '201',
    title: 'Brake inspection',
    parts: [{ name: 'Brake pads', price: 700 }],
    partsCost: 700,
    laborCost: 500
  });

  await page.goto('/offers/502');
  await expect(page.getByText('Ada Lovelace - SK-1234-AA - Volkswagen Golf')).toBeVisible();
  await expect(page.getByText('SK-1234-AA').first()).toBeVisible();
  await expect(page.getByText('Цена на делови')).toBeVisible();
  await expect(page.getByText('Brake pads')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Испрати' })).toBeDisabled();
  const offerDownloadPromise = page.waitForEvent('download');
  await page.getByRole('button', { name: 'PDF' }).click();
  const offerDownload = await offerDownloadPromise;
  expect(offerDownload.suggestedFilename()).toBe('offer-502.pdf');
  await expect.poll(() => quotePdfCalled).toBeTruthy();
  expect(sendCalled).toBe(false);
});

test('sends generated service documents and exports PDFs', async ({ page }) => {
  await signIn(page);
  let documentPdfCalled = false;
  let documentSendCalled = false;

  await page.route('**/api/documents', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok([{ id: '601', customerId: '101', serviceRecordId: '401', type: 'INSPECTION', fileName: 'Inspection report.pdf', contentType: 'application/pdf', storageKey: 'generated/inspection-report.pdf' }]))
    });
  });

  await page.route('**/api/documents/**', async (route) => {
    const pathname = new URL(route.request().url()).pathname;
    if (pathname.endsWith('/pdf')) {
      documentPdfCalled = true;
      await route.fulfill({ contentType: 'application/pdf', body: '%PDF-1.4 document' });
      return;
    }
    if (pathname.endsWith('/send')) {
      documentSendCalled = true;
    }
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({ id: '601', customerId: '101', serviceRecordId: '401', type: 'INSPECTION', fileName: 'Inspection report.pdf', contentType: 'application/pdf', storageKey: 'generated/inspection-report.pdf' }))
    });
  });

  await page.goto('/documents');
  await expect(page.getByText('Inspection report.pdf')).toBeVisible();
  await page.getByRole('button', { name: 'Преглед' }).click();
  await expect(page.locator('iframe[title="Inspection report.pdf"]')).toBeVisible();
  await expect.poll(() => documentPdfCalled).toBeTruthy();
  documentPdfCalled = false;
  const documentDownloadPromise = page.waitForEvent('download');
  await page.locator('tbody button').nth(1).click();
  const documentDownload = await documentDownloadPromise;
  expect(documentDownload.suggestedFilename()).toBe('Inspection report.pdf');
  await expect.poll(() => documentPdfCalled).toBeTruthy();
  await page.locator('tbody button').nth(2).click();
  await expect.poll(() => documentSendCalled).toBeTruthy();
});

test('creates updates and disables employee accounts from admin', async ({ page }) => {
  await signIn(page);
  const users = [admin, employee];
  let createPayload: Record<string, unknown> | undefined;
  let updatePayload: Record<string, unknown> | undefined;
  let deletedUserId: string | undefined;

  await page.route('**/api/admin/users**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    if (request.method() === 'GET') {
      await route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok(users)) });
      return;
    }
    if (request.method() === 'POST') {
      createPayload = request.postDataJSON();
      const created = { id: '3', failedLoginAttempts: 0, ...createPayload };
      users.push(created as typeof admin);
      await route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok(created)) });
      return;
    }
    if (request.method() === 'PUT') {
      updatePayload = request.postDataJSON();
      const id = url.pathname.split('/').pop() ?? '';
      const index = users.findIndex((user) => user.id === id);
      users[index] = { ...users[index], ...updatePayload };
      await route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok(users[index])) });
      return;
    }
    if (request.method() === 'DELETE') {
      deletedUserId = url.pathname.split('/').pop();
      const index = users.findIndex((user) => user.id === deletedUserId);
      users[index] = { ...users[index], enabled: false };
      await route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok(null)) });
      return;
    }
    await route.fallback();
  });

  await page.goto('/admin');
  await expect(page.locator('input[name="fullName"]')).toHaveCount(0);
  await page.getByRole('button', { name: 'Нов вработен' }).click();
  await page.locator('input[name="fullName"]').fill('New Technician');
  await page.locator('input[name="email"]').fill('new-tech@carcare.test');
  await page.locator('input[name="password"]').fill('password123');
  await page.locator('select[name="enabled"]').selectOption('true');
  await page.locator('select[name="role"]').selectOption('EMPLOYEE');
  await page.locator('button[type="submit"]').click();

  await expect(page.getByText('new-tech@carcare.test')).toBeVisible();
  expect(createPayload).toMatchObject({
    fullName: 'New Technician',
    email: 'new-tech@carcare.test',
    password: 'password123',
    enabled: true,
    roles: ['ROLE_EMPLOYEE']
  });

  await page.getByLabel('edit new-tech@carcare.test').click();
  await page.locator('input[name="fullName"]').fill('Updated Technician');
  await page.locator('select[name="enabled"]').selectOption('false');
  await page.locator('select[name="role"]').selectOption('ADMIN');
  await page.locator('button[type="submit"]').click();

  await expect(page.getByText('Updated Technician')).toBeVisible();
  expect(updatePayload).toMatchObject({
    fullName: 'Updated Technician',
    email: 'new-tech@carcare.test',
    enabled: false,
    roles: ['ROLE_ADMIN']
  });

  await page.getByLabel('delete new-tech@carcare.test').click();
  await expect(page.getByRole('dialog', { name: 'Избриши вработен' })).toBeVisible();
  await page.getByRole('button', { name: 'Избриши' }).click();
  await expect(page.getByText('DISABLED').first()).toBeVisible();
  expect(deletedUserId).toBe('3');
});

test('changes password and logs out through the authenticated shell', async ({ page }) => {
  await signIn(page);
  await page.goto('/change-password');

  await page.locator('input[name="currentPassword"]').fill('password123');
  await page.locator('input[name="newPassword"]').fill('password456');
  await page.locator('button[type="submit"]').click();

  await expect(page.getByText(/success|успешно|Ð£ÑÐ¿ÐµÑˆÐ½Ð¾/i).first()).toBeVisible();
  const successToast = page.getByTestId('app-toast');
  await expect(successToast).toHaveCount(1);
  await expect(successToast).toHaveCSS('background-color', 'rgb(21, 128, 61)');
  await expect(successToast).toHaveCSS('background-image', 'none');
  await expect(page.locator('.MuiAlert-root')).toHaveCount(0);
  await page.locator('button:has([data-testid="LogoutRoundedIcon"])').click();
  await expect(page).toHaveURL(/\/login$/);
});

test('shows an inline error when current password is wrong', async ({ page }) => {
  await signIn(page);
  await page.unroute('**/api/auth/change-password');
  await page.route('**/api/auth/change-password', async (route) => {
    await route.fulfill({ status: 401, contentType: 'application/json', body: JSON.stringify(fail('Invalid current password')) });
  });

  await page.goto('/change-password');
  await page.locator('input[name="currentPassword"]').fill('wrong-password');
  await page.locator('input[name="newPassword"]').fill('password456');
  await page.locator('button[type="submit"]').click();

  await expect(page.getByRole('alert')).toContainText('Invalid current password');
  await expect(page.getByTestId('app-toast')).toHaveCount(0);
});
