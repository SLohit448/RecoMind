# Class diagram

````mermaid
classDiagram
    class RecommendationStrategy {
        <<interface>>
        +name() String
        +score(userId, data, settings) Map
    }
    RecommendationStrategy <|.. ContentBasedStrategy
    RecommendationStrategy <|.. CollaborativeFilteringStrategy
    RecommendationStrategy <|.. PopularityStrategy
    HybridRecommender --> RecommendationStrategy : combines with admin weights
    HybridRecommender ..> Recommendation : returns
    HybridRecommender ..> RecommendationSettings : reads weights
    class RecommendationService {
        -ConcurrentHashMap cache
        -ScheduledExecutorService scheduler
        -ExecutorService writer
        +recommend(userId)
        +recordInteraction(interaction)
        +refresh()
    }
    RecommendationService --> HybridRecommender
    RecommendationService --> RecommendationData
    class TransactionManager {
        +runInTransaction(action)
        +getCurrentConnection() Connection
    }
    class AuthService {
        +login(email, password)
        +register(email, password, categories)
        +changePassword(userId, old, new)
    }
    AuthService --> UserDao
    AuthService --> PreferenceDao
    AuthService ..> TransactionManager

    class UserDao { <<interface>> }
    class ItemDao { <<interface>> }
    class CategoryDao { <<interface>> }
    class PreferenceDao { <<interface>> }
    class InteractionDao { <<interface>> }
    class SettingsDao { <<interface>> }
    class AuditDao { <<interface>> }
    UserDao <|.. UserDaoImpl
    ItemDao <|.. ItemDaoImpl
    CategoryDao <|.. CategoryDaoImpl
    PreferenceDao <|.. PreferenceDaoImpl
    InteractionDao <|.. InteractionDaoImpl
    SettingsDao <|.. SettingsDaoImpl
    AuditDao <|.. AuditDaoImpl
    UserDaoImpl ..> TransactionManager : joins open transaction
    InteractionDaoImpl ..> TransactionManager
    SettingsDaoImpl ..> TransactionManager
    RecommendationService --> InteractionDao
    RecommendationService --> ItemDao
    RecommendationService --> SettingsDao

    class Filter { <<interface>> }
    Filter <|.. AuthenticationFilter
    Filter <|.. RoleAuthorizationFilter
    Filter <|.. CsrfFilter
    Filter <|.. CharacterEncodingFilter
    class ApiServlet { <<abstract>> }
    ApiServlet ..> ApiResponse : writes JSON envelope
    AuthServlet --> AuthService
    RegisterServlet --> AuthService
    User <|-- AdminUser
    User <|-- RegularUser
    ApiServlet <|-- AdminAnalyticsServlet
    ApiServlet <|-- AdminItemsServlet
    ApiServlet <|-- AdminSettingsServlet
    ApiServlet <|-- AdminUsersServlet
    ApiServlet <|-- AuthServlet
    ApiServlet <|-- CategoryServlet
    ApiServlet <|-- CsrfServlet
    ApiServlet <|-- InteractionServlet
    ApiServlet <|-- ItemServlet
    ApiServlet <|-- PreferenceServlet
    ApiServlet <|-- RecommendationServlet
    ApiServlet <|-- RegisterServlet
```
