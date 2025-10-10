window.AceEditorUtils = {
    /**
     * Initializes the editor by disabling the default search command.
     * @param {HTMLElement} codeEditorElement - The jmix-code-editor element
     */
    initializeEditor: function (codeEditorElement) {
        const jmixShadowRoot = codeEditorElement.shadowRoot;
        if (!jmixShadowRoot) {
            console.error('Shadow root not found for code editor.');
        }
        const aceEditorDiv = jmixShadowRoot.querySelector('.ace_editor');
        if (!aceEditorDiv) {
            console.error('Ace editor div not found in shadow root.');
        }
        const editorInstance = window.ace.edit(aceEditorDiv);
        if (!editorInstance) {
            console.error('Ace Editor instance not found or invalid.');
        }

        // Disable the default Ctrl+F shortcut to prevent the built-in search panel
        this.disableDefaultSearchShortcut(editorInstance);
    },

    /**
     * Disables the default Ace Editor find command (which is tied to Ctrl+F).
     * @param {object} editorInstance - The Ace Editor instance
     */
    disableDefaultSearchShortcut: function (editorInstance) {
        editorInstance.commands.removeCommand('find');
    },

    /**
     * Finds the next occurrence of a string in the editor.
     * @param {HTMLElement} codeEditorElement - The jmix-code-editor element
     * @param {string} query - The string to search for
     * @param {boolean} backwards - Whether to search backwards
     */
    findText: function (codeEditorElement, query, backwards) {
        const editorInstance = this.getEditorInstance(codeEditorElement);
        if (!editorInstance || !query) return;

        editorInstance.find(query, {
            backwards: backwards,
            wrap: true,
            caseSensitive: false,
            wholeWord: false,
            regExp: false,
            start: editorInstance.selection.getRange()
        });
    },

    /**
     * Finds the next occurrence of a string in the editor and returns the current and total count of matches.
     * @param codeEditorElement
     * @param query
     * @param backwards
     * @returns [currentIndex, totalCount] JSON
     */
    findTextWithCount: function(codeEditorElement, query, backwards) {
        const editorInstance = this.getEditorInstance(codeEditorElement);
        if (!editorInstance || !query) return;

        const Search = window.ace.require("ace/search").Search; // Reference to the search constructor
        const search = new Search();

        // Set the search options
        search.set({
            needle: query,
            caseSensitive: false,
            wholeWord: false,
            regExp: false,
        });

        const allMatches = search.findAll(editorInstance.session);
        const totalCount = allMatches.length;

        if (totalCount === 0){
            return [0,0];
        }

        editorInstance.find(query, {
            backwards: backwards,
            wrap: true,
            caseSensitive: false,
            wholeWord: false,
            regExp: false
        });

        const newRange = editorInstance.selection.getRange();
        let currentIndex = 0;
        for (let i = 0; i < allMatches.length; i++) {
            if (newRange.start.row === allMatches[i].start.row &&
                newRange.start.column === allMatches[i].start.column) {
                currentIndex = i + 1;
                break;
            }
        }
        return [currentIndex, totalCount];
    },


    /**
     * Highlights all occurrences of a string in the editor.
     * @param codeEditorElement
     * @param query
     * @param markerCssClass
     * @param markerCssStyles
     * @param useRegex
     */
    highlightAllOccurrences: function (codeEditorElement, query, markerCssClass, markerCssStyles, useRegex) {
        const editorInstance = this.getEditorInstance(codeEditorElement);
        if (!editorInstance) return;

        const session = editorInstance.getSession();
        const markers = session.getMarkers();

        for (const id in markers) {
            if (markers[id].clazz === markerCssClass) {
                session.removeMarker(id);
            }
        }

        if (!query) return;

        // Inject CSS styles into the shadow DOm
        const shadowRoot = codeEditorElement.shadowRoot;
        const styleId = 'ace-custom-marker-styles';
        if (shadowRoot && !shadowRoot.getElementById(styleId)) {
            const style = document.createElement('style');
            style.id = styleId;
            style.textContent = markerCssStyles;
            shadowRoot.appendChild(style);
        }

        // Find and add new markers
        const Search = window.ace.require("ace/search").Search;
        const search = new Search();
        search.set({
            needle: query,
            caseSensitive: false,
            wholeWord: false,
            regExp: !!useRegex
        });

        const allMatches = search.findAll(session);
        allMatches.forEach(range => {
            session.addMarker(range, markerCssClass, "text", false);
        });
    },

    /**
     * Helper function to get the Ace-Editor instance
     */
    getEditorInstance: function(codeEditorElement) {
        if (!codeEditorElement || !codeEditorElement.shadowRoot) return null;
        const aceEditorDiv = codeEditorElement.shadowRoot.querySelector('.ace_editor');
        return aceEditorDiv ? window.ace.edit(aceEditorDiv) : null;
    }
};
