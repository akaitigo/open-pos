-- Add DISCARD, SCRAP, SAMPLE movement types (#619)
ALTER TABLE inventory_schema.stock_movements
    DROP CONSTRAINT IF EXISTS chk_movement_type;

ALTER TABLE inventory_schema.stock_movements
    ADD CONSTRAINT chk_movement_type
    CHECK (movement_type IN ('SALE', 'RETURN', 'RECEIPT', 'ADJUSTMENT', 'TRANSFER', 'DISCARD', 'SCRAP', 'SAMPLE'));
