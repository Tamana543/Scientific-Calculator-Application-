# Offline Scientific Calculator

A fully offline scientific calculator built with Java, designed to provide students with a powerful, accessible mathematical tool without requiring an internet connection.

> **Built with a simple idea:** access to useful educational tools should not depend on having internet access every day.

## About the Project

This project is a desktop scientific calculator developed in Java with the goal of creating a practical mathematical tool that can be used **completely offline**.

It was especially inspired by girls and students living in Afghanistan who may not have reliable or continuous access to the internet. Instead of depending on online calculators, websites, or cloud-based tools, this application can be installed once and then used whenever it is needed — even without an internet connection.

The project started as a simple calculator and gradually evolved into a more complete scientific-calculator application with memory functions, keyboard input, calculation history, graphing, matrices, persistent state, and other features found in modern calculators.

---

## Features

### Calculator

- Basic arithmetic operations
- Scientific calculations
- Parentheses and operator handling
- `+/-` sign toggle
- Percentage calculations
- `AC` (All Clear)
- `DEL` (Delete)
- `EXE` (Execute)
- Keyboard input support

### Alpha & Shift Functions

The calculator includes dedicated **ALPHA** and **SHIFT** functionality.

- Store values using memory variables
- Recall stored values
- Use variables directly in calculations
- Visual indicators for functions assigned to buttons
- Memory values can persist between application sessions

### Calculation History

The application keeps track of previous calculations.

- View previous calculations
- Click a previous calculation to recall it
- Edit recalled expressions
- Re-run previous calculations

### Keyboard Support

The calculator can be operated without relying entirely on mouse input.

| Keyboard | Calculator |
|---|---|
| `0–9` | Numbers |
| `+ - * /` | Operators |
| `Enter` | `EXE` |
| `Backspace` | `DEL` |
| `Esc` | `AC` |

### Graphing

The GRAPH mode allows mathematical functions to be visualized directly inside the application.

#### Multiple Functions

Multiple functions can be plotted on the same graph, allowing users to compare functions visually.

For example:

```text
f₁(x) = x²
f₂(x) = 2x + 1
```

#### Zoom & Pan

The graph can be explored interactively:

- Scroll to zoom
- Drag to move around the graph
- Explore different regions of a function

#### Trace Mode

Clicking on a point of a plotted function displays its coordinates.

```text
x = ...
y = ...
```

### Matrix Mode

The MATRIX mode provides basic matrix functionality.

Currently supported operations include:

- 2×2 matrices
- 3×3 matrices
- Matrix entry
- Matrix addition
- Matrix multiplication
- Determinant calculation

### Persistent State

The calculator can preserve important information between sessions.

Stored information can include:

- Memory variables
- Stored values
- Calculation history
- Application state

