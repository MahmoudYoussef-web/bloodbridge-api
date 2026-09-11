-- V7__Egypt_region_data.sql
-- Switch demo region data from Gaza Strip to Egypt (Cairo-centered).
-- Governorates are matched by their previous English names so this
-- migration is a no-op on databases seeded with other region data.

UPDATE governorates SET name_en = 'Cairo', name_ar = 'القاهرة'
WHERE name_en = 'Gaza';

UPDATE governorates SET name_en = 'Giza', name_ar = 'الجيزة'
WHERE name_en = 'North Gaza';

UPDATE governorates SET name_en = 'Alexandria', name_ar = 'الإسكندرية'
WHERE name_en = 'Deir al-Balah';

UPDATE governorates SET name_en = 'Dakahlia', name_ar = 'الدقهلية'
WHERE name_en = 'Khan Yunis';

UPDATE governorates SET name_en = 'Qalyubia', name_ar = 'القليوبية'
WHERE name_en = 'Rafah';

UPDATE settings
SET payload = '{"value": "+20-100-123-4567"}', updated_at = NOW()
WHERE group_name = 'general' AND name = 'support_phone'
  AND payload LIKE '%+970%';

UPDATE settings
SET payload = '{"value": 30.0444}', updated_at = NOW()
WHERE group_name = 'general' AND name = 'map_default_lat'
  AND payload LIKE '%31.5%';

UPDATE settings
SET payload = '{"value": 31.2357}', updated_at = NOW()
WHERE group_name = 'general' AND name = 'map_default_lng'
  AND payload LIKE '%34.4667%';
