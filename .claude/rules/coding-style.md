# Coding Style Guidelines

## Overview

This file covers code quality, patterns, style, and organization principles. These guidelines apply across all languages and technologies.

## Principles

### Principle 1: Line Endings

**Why**: Consistent line endings prevent merge conflicts and display issues.

**Rule**: ALWAYS use Unix line endings (LF), never Windows/DOS style (CRLF).

### Principle 2: Immutability

**Why**: Immutable data prevents side effects and makes code easier to reason about.

**Rule**: ALWAYS create new objects, NEVER mutate existing ones.

**Examples**:
- TypeScript/JavaScript:
  ```javascript
  // WRONG: Mutation
  function updateUser(user, name) {
    user.name = name  // MUTATION!
    return user
  }

  // CORRECT: Immutability
  function updateUser(user, name) {
    return { ...user, name }
  }
  ```
- C#:
  ```csharp
  // CORRECT: Use records for immutability
  public record User(string Name, string Email);
  var updated = user with { Name = newName };
  ```
- Java:
  ```java
  // CORRECT: Return new instance
  public User withName(String newName) {
    return new User(newName, this.email);
  }
  ```

### Principle 3: File Organization

**Why**: Small, focused files are easier to understand, test, and maintain.

**Rule**: MANY SMALL FILES > FEW LARGE FILES
- High cohesion, low coupling
- 200-400 lines typical, 800 max
- Extract utilities from large components
- Organize by feature/domain, not by type

### Principle 4: Function Size

**Why**: Small functions are easier to understand, test, and reuse.

**Rule**:
- Functions should do one thing
- Target <50 lines per function
- Extract complex logic into helper functions
- Avoid deep nesting (>4 levels)

### Principle 5: Naming

**Why**: Clear names reduce the need for comments and documentation.

**Rule**:
- Use descriptive, intention-revealing names
- Prefer clarity over brevity
- Use domain terminology consistently
- Avoid abbreviations except well-known ones

### Principle 6: Design Patterns

**Why**: Consistent patterns make code predictable and maintainable.

**Rule**: Apply these patterns appropriately:
- **Domain-Driven Design (DDD)** for business logic modeling
- **Hexagonal Architecture** (Ports & Adapters) for infrastructure abstraction
- **Composition over Inheritance**
- **SOLID Principles**
- **Dependency Injection** for testability and loose coupling

### Principle 7: Code Clarity

**Why**: Code is read more often than written.

**Rule**:
- Easy understanding > cleverness
- Avoid premature optimization
- Use standard library over custom solutions
- Write self-documenting code

### Principle 8: Dependency Management

**Why**: Dependencies introduce risk and maintenance burden.

**Rule**:
- Always use the **latest stable versions** of all dependencies
- Regularly check for updates and security advisories
- Avoid deprecated or unmaintained libraries
- Prefer built-in language/framework libraries over external packages
- Verify dependency integrity and review changelogs before upgrades

## Code Quality Checklist

Before marking work complete:
- [ ] Code is readable and well-named
- [ ] Functions are small (<50 lines)
- [ ] Files are focused (<800 lines)
- [ ] No deep nesting (>4 levels)
- [ ] Proper error handling
- [ ] No console.log/print statements left in production code
- [ ] No hardcoded values (use configuration)
- [ ] No mutation (immutable patterns used)
- [ ] Dependencies are justified and up-to-date
