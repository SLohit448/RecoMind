# ER diagram

````mermaid
erDiagram
    ROLES ||--o{ USERS : "has"
    USERS ||--o| USER_PREFERENCES : "has"
    USERS ||--o{ INTERACTIONS : "makes"
    ITEMS ||--o{ INTERACTIONS : "receives"
    CATEGORIES ||--o{ ITEMS : "groups"
    USERS ||--o{ RECOMMENDATIONS : "gets"
    ITEMS ||--o{ RECOMMENDATIONS : "appears in"
    USERS ||--o{ AUDIT_LOG : "triggers"

    ROLES {
        bigint ID PK
        varchar NAME UK
    }
    USERS {
        bigint ID PK
        varchar EMAIL UK
        varchar PASSWORD_HASH
        boolean ACTIVE
        bigint ROLE_ID FK
    }
    USER_PREFERENCES {
        bigint USER_ID PK
        clob PREFERENCE_JSON
    }
    CATEGORIES {
        bigint ID PK
        varchar NAME UK
    }
    ITEMS {
        bigint ID PK
        varchar TITLE
        bigint CATEGORY_ID FK
        varchar TAGS
        clob DESCRIPTION
        varchar IMAGE_URL
        decimal PRICE
    }
    INTERACTIONS {
        bigint ID PK
        bigint USER_ID FK
        bigint ITEM_ID FK
        varchar TYPE
        int RATING
        timestamp CREATED_AT
    }
    RECOMMENDATIONS {
        bigint USER_ID PK
        bigint ITEM_ID PK
        double SCORE
    }
    SYSTEM_SETTINGS {
        varchar SETTING_KEY PK
        varchar SETTING_VALUE
    }
    AUDIT_LOG {
        bigint ID PK
        bigint USER_ID FK
        varchar ACTION
        timestamp TIMESTAMP
        clob DETAILS
    }
````

`SYSTEM_SETTINGS` is a key/value table (algorithm weights, N, threshold, interval) and has no foreign keys.