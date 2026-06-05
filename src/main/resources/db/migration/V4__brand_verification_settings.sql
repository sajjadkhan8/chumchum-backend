alter table brands
    add column business_verification_status varchar(50),
    add column verification_contact_email varchar(255),
    add column verification_phone_number varchar(50);
