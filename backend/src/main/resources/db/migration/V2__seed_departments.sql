-- Seed the municipal departments used by ComplaintService category routing.
-- The unique department code makes this safe to run again without changing
-- an existing department record.
INSERT INTO departments (name, code, description)
VALUES
    ('Solid Waste Management', 'SWM', 'Handles garbage collection and solid waste services.'),
    ('Roads and Infrastructure', 'ROADS', 'Handles roads, potholes, and related public infrastructure.'),
    ('Drainage and Sewerage', 'DRAIN', 'Handles drainage problems and sewerage maintenance.'),
    ('Electrical and Street Lighting', 'ELEC', 'Handles municipal electrical infrastructure and streetlight issues.'),
    ('Water Supply', 'WATER', 'Handles municipal water supply issues.')
ON CONFLICT (code) DO NOTHING;
