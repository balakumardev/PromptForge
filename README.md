# 🚀 PromptForge - Context-Rich Prompts for LLMs, Effortlessly

Right-click, paste, get answers. It's that simple.

[![License: GPL-3.0](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![JetBrains Plugin](https://img.shields.io/badge/JetBrains-Plugin-orange.svg)](https://plugins.jetbrains.com/)
[![Java](https://img.shields.io/badge/Language-Java-red.svg)](https://www.java.com/)

**PromptForge gives your LLM the full context it needs with a single click.** No more copy-pasting multiple files, just right-click and paste for accurate, context-aware responses.

## 💥 The Problem

When working with LLMs on code-related tasks, context is everything:

1. Copy a single file to ChatGPT
2. Get the response: *"I need to see the related classes to help you"*
3. Copy another file
4. Get: *"I still need to see how this interfaces with XYZ"*
5. Repeat until you've shared enough context

While IDE-integrated AI assistants are improving, they often have to make trade-offs about what context to include due to token limits and cost considerations. Sometimes you just want full control over what context is provided.

## ✨ The Solution: PromptForge

PromptForge is a simple yet powerful plugin that gives you complete control over the context you provide to LLMs:

- **One-Click Operation** - Right-click, select action, paste to LLM
- **Complete Context** - Automatically includes related files and dependencies
- **Full Control** - You decide how much context to include
- **No API Keys** - Works with any LLM you prefer
- **Task-Specific Prompts** - Optimized templates for different use cases

## 🛠️ What It Does

- **Gathers Related Files** - Automatically includes imports, dependencies, and implementations
- **Preserves Context** - Keeps the relationships between your files intact
- **Formats Prompts** - Creates optimized prompts for specific tasks:
    - 🧪 **Generate Tests** - For new or modified code
    - 📝 **Explain Code** - For understanding complex code
    - ❓ **Ask Questions** - For specific queries about your code
    - 🔄 **Make Changes** - For implementing specific changes
    - 📋 **Copy with Context** - For custom prompts


## ⚙️ Installation

### From JetBrains Marketplace (SOON, NOT PUBLISHED YET)

1. In IntelliJ IDEA, go to `Settings` → `Plugins` → `Marketplace`
2. Search for "PromptForge"
3. Click "Install"
4. Restart IntelliJ IDEA when prompted

### Manual Installation

1. Download the latest release ZIP file from the [Releases page](https://github.com/balakumardev/PromptForge/releases)
2. In IntelliJ IDEA, go to `Settings` → `Plugins`
3. Click the gear icon (⚙️) and select `Install Plugin from Disk...`
4. Choose the downloaded ZIP file
5. Restart IntelliJ IDEA when prompted
6. 
## 📖 Usage

### The Basic Workflow

1. Right-click on a file in your IDE
2. Select a PromptForge action from the context menu
3. The plugin collects all related context
4. Paste the result into your favorite LLM
5. Get accurate, contextual responses the first time

### Generate Tests

Perfect for TDD or adding tests to existing code. Detects if you're working with a new file or making changes to an existing one.

### Explain Code

When you're staring at a complex piece of code thinking "what does this even do?" Let an LLM explain it with full context.

### Ask Questions

Have a specific question about your code? This action lets you ask it while providing all the context the LLM needs to answer correctly.

### Make Changes

Need to implement a feature or fix a bug? This action helps you get accurate suggestions based on your entire codebase's context.

### Copy with Context

For power users who want to craft their own prompts but still need the related files and context.

## ⚙️ Configuration

Go to `Settings` → `Tools` → `PromptForge` to configure:

- How deep to search for related files
- Which packages to exclude
- Custom prompt templates
- Content pruning options

## 🔧 Requirements

- IntelliJ IDEA 2023.1 or later
- Java 17 or later
- Git installed and configured (for modified file detection)

## 🤝 Contributing

Found a bug? Have a feature request? Contributions are welcome!

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.

## ⭐ Support

If you find this plugin useful, please:
- Give it a star on GitHub
- Share it with your network
- Report issues or suggest improvements

## 📫 Contact

Bala Kumar - [mail@balakumar.dev](mailto:mail@balakumar.dev)