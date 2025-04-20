<script>
    import { Input } from "$lib/components/ui/input";
    import { Button } from "$lib/components/ui/button";
    import * as Card from "$lib/components/ui/card/index.js";
    import { Separator } from "$lib/components/ui/separator";
    import * as Checkbox from "$lib/components/ui/checkbox";
    import { PUBLIC_API_URL } from "$env/static/public";
    import { onMount, onDestroy } from 'svelte';

    let searchQuery = "";
    let searchResults = [];
    let loading = false;
    let semanticLoading = false;
    let searchDebounceTimeout;
    let semanticDebounceTimeout;
    let urlUpdateTimeout; // New timeout for URL updates
    let tagLabels = {}; // Store tag IDs and their labels
    let fetchedTagIds = new Set(); // Track which tag IDs we've attempted to fetch
    let browserSide = false; // Flag to check if we're in the browser
    let isInitialLoad = true; // Track if this is the initial load
    let searchPending = false; // Track if a search is already pending
    
    // Semantic search additions
    let semanticExpansions = [];
    let selectedExpansions = [];
    let showExpansions = false;
    let allVisibleExpansions = []; // This will hold all expansions that should be visible

    // Define a list of mystery object attributes to check for matches
    const mysteryObjectAttributes = [
        { key: "material", label: "Material" },
        { key: "writtenText", label: "Written Text" },
        { key: "color", label: "Color" },
        { key: "shape", label: "Shape" },
        { key: "location", label: "Location" },
        { key: "hardness", label: "Hardness" },
        { key: "timePeriod", label: "Time Period" },
        { key: "smell", label: "Smell" },
        { key: "texture", label: "Texture" },
        { key: "originOfAcquisition", label: "Origin" },
        { key: "pattern", label: "Pattern" },
        { key: "print", label: "Print" },
        { key: "functionality", label: "Functionality" },
        { key: "brand", label: "Brand" },
        { key: "markings", label: "Markings" }
    ];

    // Initialize the component when mounted
    onMount(() => {
        browserSide = true;
        
        // If there's an initial search query in URL params, load it
        const urlParams = new URLSearchParams(window.location.search);
        const initialQuery = urlParams.get('q');
        if (initialQuery && initialQuery.trim().length >= 3) {
            searchQuery = initialQuery;
            loading = true; // Set loading state for initial query
            
            // Wait for next tick before triggering search (prevents immediate reactivity issues)
            setTimeout(() => {
                isInitialLoad = false;
                // The reactive statement will trigger the search
            }, 0);
        } else {
            isInitialLoad = false;
        }
    });

    // Clean up timeouts when component is destroyed
    onDestroy(() => {
        if (searchDebounceTimeout) clearTimeout(searchDebounceTimeout);
        if (semanticDebounceTimeout) clearTimeout(semanticDebounceTimeout);
        if (urlUpdateTimeout) clearTimeout(urlUpdateTimeout);
    });

    function containsSearchTerms(text, query) {
        if (!text || !query) return false;
        const trimmedQuery = query.trim().toLowerCase();
        return text.toLowerCase().includes(trimmedQuery);
    }

    // Check if text contains any of the search terms (original or expansions)
    function containsAnySearchTerms(text) {
        if (!text) return false;
        
        // Check the main search query
        if (containsSearchTerms(text, searchQuery)) return true;
        
        // Check expansion terms
        for (const exp of selectedExpansions) {
            if (containsSearchTerms(text, exp.label)) return true;
        }
        
        return false;
    }

    // IMPROVEMENT 1: Score function to rank suggestion relevance
    function computeRelevanceScore(label, query, relation) {
        let score = 0;
        
        // Exact match gets highest score
        if (label.toLowerCase() === query.toLowerCase()) {
            score += 100;
        }
        // Partial word match
        else if (label.toLowerCase().includes(query.toLowerCase())) {
            score += 70;
        }
        // Individual words match
        else {
            const queryWords = query.toLowerCase().split(/\s+/);
            const labelWords = label.toLowerCase().split(/\s+/);
            
            for (const word of queryWords) {
                if (word.length < 3) continue; // Skip short words
                for (const labelWord of labelWords) {
                    if (labelWord.includes(word) || word.includes(labelWord)) {
                        score += 20;
                        break;
                    }
                }
            }
        }
        
        // Adjust score based on relation type (prioritize certain relationships)
        switch (relation) {
            case "subclass": 
                score += 15; 
                break;
            case "parent class": 
                score += 10; 
                break;
            case "made of":
            case "color":
            case "has part":
                score += 25; // Physical properties highly relevant for objects
                break;
            case "used for":
                score += 20;
                break;
            case "also known as":
                score += 30; // Alternative names are very useful
                break;
            default: 
                score += 5;
        }
        
        // Shorter labels are generally more useful
        if (label.length < 15) {
            score += 10;
        }
        
        return score;
    }

    // IMPROVEMENT 1: Enhanced expandSearchTerms function for multiple entities
    async function expandSearchTerms(query) {
        if (!query || query.length < 3) return [];
        
        try {
            semanticLoading = true;
            
            // Retrieve multiple initial entities instead of just one
            const searchResponse = await fetch(
                `https://www.wikidata.org/w/api.php?action=wbsearchentities&search=${encodeURIComponent(query)}&language=en&format=json&origin=*&limit=3`
            );
            
            if (!searchResponse.ok) {
                throw new Error(`Wikidata search failed: ${searchResponse.status}`);
            }
            
            const searchData = await searchResponse.json();
            
            if (!searchData.search || !searchData.search.length) return [];
            
            // Process multiple entities to get more diverse results
            const topEntities = searchData.search.slice(0, 3);
            console.log("Top Wikidata entities found:", topEntities.map(e => `${e.id} (${e.label})`));
            
            // Create a map to track unique results and avoid duplicates
            const uniqueResults = new Map();
            
            // Process each entity in parallel for better performance
            await Promise.all(topEntities.map(async (entity) => {
                // More comprehensive SPARQL query with physical object relevant properties
                const sparqlQuery = `
                    SELECT DISTINCT ?item ?itemLabel ?relation WHERE {
                    {
                        # Parent classes (instance of)
                        wd:${entity.id} wdt:P31 ?item .
                        BIND("parent class" AS ?relation)
                    } UNION {
                        # Subclasses
                        ?item wdt:P31 wd:${entity.id} .
                        BIND("subclass" AS ?relation)
                    } UNION {
                        # Related concepts through subclass hierarchy
                        wd:${entity.id} wdt:P279 ?item .
                        BIND("broader concept" AS ?relation)
                    } UNION {
                        # Parts and components
                        wd:${entity.id} wdt:P527 ?item .
                        BIND("has part" AS ?relation)
                    } UNION {
                        # What it's part of
                        wd:${entity.id} wdt:P361 ?item .
                        BIND("part of" AS ?relation)
                    } UNION {
                        # Material composition
                        wd:${entity.id} wdt:P186 ?item .
                        BIND("made of" AS ?relation)
                    } UNION {
                        # Object's color
                        wd:${entity.id} wdt:P462 ?item .
                        BIND("color" AS ?relation)
                    } UNION {
                        # Used for
                        wd:${entity.id} wdt:P366 ?item .
                        BIND("used for" AS ?relation)
                    }
                    SERVICE wikibase:label { bd:serviceParam wikibase:language "en". }
                    }
                    LIMIT 12
                `;
                
                const url = `https://query.wikidata.org/sparql?query=${encodeURIComponent(sparqlQuery)}&format=json`;
                
                // Add timeout for the request
                const controller = new AbortController();
                const timeoutId = setTimeout(() => controller.abort(), 5000); // 5-second timeout
                
                try {
                    const response = await fetch(url, { signal: controller.signal });
                    clearTimeout(timeoutId);
                    
                    if (!response.ok) {
                        throw new Error(`SPARQL query failed: ${response.status}`);
                    }
                    
                    const data = await response.json();
                    
                    if (!data.results || !data.results.bindings) {
                        return;
                    }
                    
                    // Process the results
                    data.results.bindings
                        .filter(item => item.item && item.itemLabel && item.relation)
                        .forEach(item => {
                            const id = item.item.value.split('/').pop();
                            const label = item.itemLabel.value;
                            const relation = item.relation.value;
                            
                            // Skip items with very long labels
                            if (label.length > 50) return;
                            
                            // Add to unique results map if not already present
                            if (!uniqueResults.has(id)) {
                                uniqueResults.set(id, {
                                    id,
                                    label,
                                    relation,
                                    score: computeRelevanceScore(label, query, relation),
                                    sourceEntity: entity.id
                                });
                            }
                        });
                } catch (error) {
                    if (error.name === 'AbortError') {
                        console.error('SPARQL query timed out');
                    }
                    // Continue with other entities
                }
            }));
            
            // Score and sort results
            const results = Array.from(uniqueResults.values())
                .sort((a, b) => b.score - a.score)
                .slice(0, 10); // Limit to top 10 most relevant results
            
            console.log("Found semantic expansions:", results);
            return results;
        } catch (error) {
            console.error('Error expanding search terms:', error);
            return [];
        } finally {
            semanticLoading = false;
        }
    }

    // Build the combined search query with all selected expansion terms
    function buildExpandedQuery() {
        if (selectedExpansions.length === 0) return searchQuery.trim();
        
        // Combine the original query with selected expansion terms
        const expandedTerms = selectedExpansions.map(exp => exp.label);
        return `${searchQuery.trim()},${expandedTerms.join(',')}`;
    }

    // FIXED: Improved performSearch function with proper flag handling
    async function performSearch() {
        if (searchPending) return; // Prevent concurrent searches
        
        if (searchDebounceTimeout) {
            clearTimeout(searchDebounceTimeout);
        }
        
        searchPending = true;
        
        try {
            // Only search if query has at least 3 characters
            if (!searchQuery || searchQuery.trim().length < 3) {
                searchResults = [];
                loading = false;
                return;
            }
            
            loading = true;
            
            // Store the current query to check if it's still valid at the end
            const currentQuery = searchQuery.trim();
            
            // Build the expanded search query
            const expandedQuery = buildExpandedQuery();
            console.log("Searching with expanded query:", expandedQuery);
            
            try {
                // Use the backend endpoint
                const response = await fetch(`${PUBLIC_API_URL}/api/posts/searchForPosts?q=${encodeURIComponent(expandedQuery)}`);
                
                if (!response.ok) {
                    throw new Error(`Search request failed: ${response.status}`);
                }
                
                // Only update if the query is still the same
                if (currentQuery === searchQuery.trim()) {
                    const data = await response.json();
                    searchResults = data.content || [];
                    
                    // Collect all unique tag IDs that we haven't fetched yet
                    const tagIds = new Set();
                    searchResults.forEach(post => {
                        if (post.tags && Array.isArray(post.tags) && post.tags.length > 0) {
                            post.tags.forEach(tag => {
                                if (tag && !fetchedTagIds.has(tag)) {
                                    tagIds.add(tag);
                                }
                            });
                        }
                    });
                    
                    // Fetch labels for tags we haven't seen before
                    if (tagIds.size > 0) {
                        await fetchTagLabels(Array.from(tagIds));
                    }
                    
                    console.log("Search results:", searchResults);
                }
            } catch (error) {
                console.error("Error searching posts:", error);
                searchResults = [];
            }
        } finally {
            loading = false;
            searchPending = false;
        }
    }

    // FIXED: Improved updateSemanticSearch with better state management
    async function updateSemanticSearch() {
        if (semanticLoading) return; // Prevent concurrent semantic searches
        
        if (semanticDebounceTimeout) {
            clearTimeout(semanticDebounceTimeout);
        }
        
        const currentQuery = searchQuery.trim();
        
        // Set the loading flag before the debounce to prevent multiple calls
        semanticLoading = true;
        
        semanticDebounceTimeout = setTimeout(async () => {
            try {
                if (!currentQuery || currentQuery.length < 3 || currentQuery !== searchQuery.trim()) {
                    semanticExpansions = [];
                    updateVisibleExpansions();
                    showExpansions = allVisibleExpansions.length > 0;
                    return;
                }
                
                const expansions = await expandSearchTerms(currentQuery);
                
                // Only update if the query is still the same
                if (currentQuery === searchQuery.trim()) {
                    semanticExpansions = expansions;
                    updateVisibleExpansions();
                    showExpansions = allVisibleExpansions.length > 0;
                }
            } catch (error) {
                console.error("Error in semantic search:", error);
            } finally {
                semanticLoading = false;
            }
        }, 500); // Slightly longer debounce for semantic search
    }

    // Toggle selection of a semantic expansion term
    function toggleExpansionTerm(term) {
        const index = selectedExpansions.findIndex(exp => exp.id === term.id);
        
        if (index !== -1) {
            selectedExpansions = selectedExpansions.filter(exp => exp.id !== term.id);
        } else {
            selectedExpansions = [...selectedExpansions, term];
        }
        
        // Update visible expansions list
        updateVisibleExpansions();
        
        // Update search with new expansions (use setTimeout to break reactive chain)
        setTimeout(() => {
            performSearch();
        }, 0);
    }
    
    // FIXED: Improved updateVisibleExpansions to prevent unnecessary updates
    function updateVisibleExpansions() {
        // Create a copy to avoid direct reactivity
        const newVisibleExpansions = [...semanticExpansions]
            .filter(exp => !exp.label.match(/^Q\d+$/)); // Filter out raw Q-IDs
        
        // Add selected expansions not already in the list
        selectedExpansions.forEach(selected => {
            if (!newVisibleExpansions.some(exp => exp.id === selected.id) && 
                !selected.label.match(/^Q\d+$/)) { // Only add if not a raw Q-ID
                newVisibleExpansions.push({
                    ...selected,
                    fromPreviousSearch: true
                });
            }
        });
        
        // Only update if there's an actual change to avoid triggering reactivity
        if (JSON.stringify(allVisibleExpansions) !== JSON.stringify(newVisibleExpansions)) {
            allVisibleExpansions = newVisibleExpansions;
        }
    }

    // Check if an expansion term is selected
    function isExpansionSelected(term) {
        return selectedExpansions.some(exp => exp.id === term.id);
    }

    async function fetchTagLabels(tagIds) {
        if (!tagIds || !tagIds.length) return;
        
        try {
            // Mark these IDs as fetched to avoid duplicate requests
            tagIds.forEach(id => fetchedTagIds.add(id));
            
            // Batch fetch labels for tags (max 20 at a time to avoid URL length issues)
            const BATCH_SIZE = 20;
            
            for (let i = 0; i < tagIds.length; i += BATCH_SIZE) {
                const batch = tagIds.slice(i, i + BATCH_SIZE);
                const validBatch = batch.filter(id => id && typeof id === 'string');
                
                if (validBatch.length === 0) continue;
                
                const queryString = validBatch.join('|');
                
                const controller = new AbortController();
                const timeoutId = setTimeout(() => controller.abort(), 5000);
                
                try {
                    const response = await fetch(
                        `https://www.wikidata.org/w/api.php?action=wbgetentities&ids=${queryString}&props=labels&languages=en&format=json&origin=*`,
                        { signal: controller.signal }
                    );
                    
                    clearTimeout(timeoutId);
                    
                    if (!response.ok) {
                        throw new Error(`Failed to fetch tag labels: ${response.status}`);
                    }
                    
                    const data = await response.json();
                    
                    // Process the response to extract labels
                    if (data.entities) {
                        Object.keys(data.entities).forEach(id => {
                            const entity = data.entities[id];
                            if (entity.labels && entity.labels.en) {
                                tagLabels[id] = entity.labels.en.value;
                            } else {
                                tagLabels[id] = id; // Fallback to ID if no label found
                            }
                        });
                    }
                } catch (error) {
                    if (error.name === 'AbortError') {
                        console.error('Tag label fetch timed out');
                    } else {
                        console.error(`Error fetching batch of tag labels: ${error.message}`);
                    }
                    // Mark failed IDs with their ID as label so we don't retry
                    validBatch.forEach(id => {
                        if (!tagLabels[id]) tagLabels[id] = id;
                    });
                }
            }
        } catch (error) {
            console.error("Error fetching tag labels:", error);
            // Even on error, we've marked these as fetched to avoid retrying constantly
        }
    }

    // Create regex patterns once when search terms change
    let highlightRegex = null;
    
    function updateHighlightRegex() {
        // Create a regex that matches any of the search terms
        let queryTerms = [searchQuery.trim()];
        
        // Add expansion terms
        selectedExpansions.forEach(exp => {
            queryTerms.push(exp.label);
        });
        
        // Filter out empty terms and escape regex special characters
        queryTerms = queryTerms.filter(term => term && term.trim().length > 0)
            .map(term => term.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'));
        
        if (queryTerms.length === 0) {
            highlightRegex = null;
            return;
        }
        
        // Create a regex that matches any of the terms
        const pattern = queryTerms.join('|');
        highlightRegex = new RegExp(`(${pattern})`, 'gi');
    }

    function highlightMatches(text) {
        if (!text || !highlightRegex) return text;
        return text.replace(highlightRegex, "<mark>$1</mark>");
    }

    function getTagLabel(tagId) {
        return tagLabels[tagId] || tagId;
    }

    // FIXED: Improved form submission handler
    function handleSubmit(event) {
        event.preventDefault();
        
        // Only trigger search if not already searching
        if (!searchPending && !semanticLoading && searchQuery.trim().length >= 3) {
            performSearch();
            updateSemanticSearch();
        }
    }

    // FIXED: Improved main reactive statement to prevent loops
    // Add this variable to track the last query that was searched
    let lastSearchedQuery = '';

    // Replace the existing reactive statement with this improved version
    $: if (searchQuery && searchQuery.trim().length >= 3 && !isInitialLoad) {
        // Only trigger search if query changed and we're not already searching
        const currentQuery = searchQuery.trim();
        if (currentQuery !== lastSearchedQuery && !searchPending && !semanticLoading) {
            // Store the query we're about to search for
            lastSearchedQuery = currentQuery;
            
            clearTimeout(searchDebounceTimeout);
            searchDebounceTimeout = setTimeout(() => {
                performSearch();
                
                if (searchQuery.trim() === currentQuery) {
                    updateSemanticSearch();
                }
            }, 300);
        }
    } else if (!searchQuery || searchQuery.trim().length < 3) {
        // Clear results if search is too short (only if not already searching)
        if (!searchPending) {
            searchResults = [];
            loading = false;
        }
    }
    
    // Update highlight regex when search terms change
    $: {
        if (searchQuery || selectedExpansions.length > 0) {
            updateHighlightRegex();
        } else {
            highlightRegex = null;
        }
    }
    
    // FIXED: Improved URL update logic with debouncing and better checks
    $: if (browserSide && typeof window !== 'undefined' && !isInitialLoad) {
        // Capture the current query value
        const currentQuery = searchQuery.trim();
        
        // Only proceed if the query is different from the last update
        if (currentQuery !== lastUrlUpdate) {
            // Update the lastUrlUpdate tracking variable
            lastUrlUpdate = currentQuery;
            
            // Debounce the URL update
            clearTimeout(urlUpdateTimeout);
            urlUpdateTimeout = setTimeout(() => {
                try {
                    const url = new URL(window.location.href);
                    if (currentQuery.length >= 3) {
                        url.searchParams.set('q', currentQuery);
                    } else {
                        url.searchParams.delete('q');
                    }
                    window.history.replaceState({}, '', url.toString());
                } catch (e) {
                    console.error("Error updating URL:", e);
                }
            }, 500); // Longer debounce for URL updates
        }
    }
    // Initialize lastUrlUpdate
    let lastUrlUpdate = '';
</script>

<div class="flex flex-col items-center h-full min-h-screen text-text bg-change dark:bg-dark shifting p-4 lg:py-8">
  <div class="w-full max-w-7xl">
    <Card.Root class="bg-white dark:bg-neutral-950 shadow-md rounded-md border border-neutral-200 dark:border-neutral-800 overflow-hidden">
      <Card.Title class="p-6 text-2xl font-semibold text-center">
        Enhanced Semantic Search
        <small class="block text-sm mt-2 font-normal text-neutral-500 dark:text-neutral-400">
          Discover objects while leveraging semantic search with Wikidata
        </small>
      </Card.Title>
      
      <div class="px-4 sm:px-6 pb-6">
        <!-- Search Form with submission handling -->
        <form on:submit={handleSubmit} class="mb-6">
          <div class="relative">
            <Input
              type="search"
              placeholder="Enter search terms..."
              bind:value={searchQuery}
              class="w-full pl-10 py-3 text-base border-neutral-300 dark:border-neutral-700 rounded-full focus:ring-teal-500 focus:border-teal-500"
              aria-label="Search terms"
            />
            <div class="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-neutral-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </div>
            <button type="submit" class="sr-only">Search</button>
          </div>
        </form>
        
        <!-- IMPROVEMENT 2: Enhanced Semantic Expansions Section with Relevance Indicators -->
        {#if semanticLoading}
          <div class="flex justify-center my-6 py-4">
            <div class="w-6 h-6 border-2 border-teal-600 border-t-transparent rounded-full animate-spin" 
                 aria-label="Loading related terms"></div>
            <span class="ml-3 text-neutral-600 dark:text-neutral-300">Finding related terms...</span>
          </div>
        {:else if showExpansions}
          <div class="mb-6 bg-neutral-50 dark:bg-neutral-900/50 p-4 rounded-lg border border-neutral-200 dark:border-neutral-800 shadow-sm">
            <h3 class="text-sm font-semibold mb-3 text-neutral-700 dark:text-neutral-200 flex items-center">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1 text-teal-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
              Related Terms from Wikidata:
            </h3>
            
            <!-- Group by relation type for better organization -->
            {#if allVisibleExpansions.length > 0}
              <!-- Get unique relation types -->
              {@const relationTypes = [...new Set(allVisibleExpansions
                .filter(exp => !exp.label.match(/^Q\d+$/))
                .map(exp => exp.relation))]}
              
              <div class="space-y-4">
                {#each relationTypes as relationType}
                  <div>
                    <h4 class="text-xs uppercase tracking-wider font-medium mb-2 text-teal-700 dark:text-teal-400 border-l-2 border-teal-500 pl-2">{relationType}:</h4>
                    <div class="flex flex-wrap gap-2">
                      {#each allVisibleExpansions
                      .filter(exp => exp.relation === relationType && !exp.label.match(/^Q\d+$/)) as expansion (expansion.id)}
                        <div class="flex items-center">
                          <!-- svelte-ignore a11y-label-has-associated-control -->
                          <label class="flex items-center space-x-2 px-2.5 py-1.5 bg-white dark:bg-neutral-800 rounded-full border border-neutral-200 dark:border-neutral-700 cursor-pointer hover:bg-neutral-100 dark:hover:bg-neutral-700 transition-colors duration-200 shadow-sm"
                                 class:border-teal-500={expansion.fromPreviousSearch || isExpansionSelected(expansion)}
                                 class:bg-teal-50={isExpansionSelected(expansion)}
                                 class:shadow-md={isExpansionSelected(expansion)}
                                 class:bg-opacity-30={expansion.fromPreviousSearch}
                                 class:border-l-4={expansion.score > 70}
                                 class:border-l-teal-600={expansion.score > 70}
                                 title={`Relevance: ${expansion.score > 80 ? 'High' : expansion.score > 50 ? 'Medium' : 'Low'}`}>
                            <Checkbox.Root
                              checked={isExpansionSelected(expansion)}
                              onCheckedChange={() => toggleExpansionTerm(expansion)}
                              class="data-[state=checked]:bg-teal-600 border-2 border-neutral-300 dark:border-neutral-600 data-[state=checked]:border-teal-600 size-4"
                            >
                              <Checkbox.Indicator>
                                <svg width="12" height="12" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                                  <path d="M13.3334 4L6.00002 11.3333L2.66669 8" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" />
                                </svg>
                              </Checkbox.Indicator>
                            </Checkbox.Root>
                            <span class="text-xs font-medium flex items-center">
                              <span>{expansion.label}</span>
                              
                              <!-- Relevance indicator -->
                              {#if expansion.score > 70}
                                <span class="ml-1 text-teal-600 dark:text-teal-400">★</span>
                              {/if}
                              
                              {#if expansion.fromPreviousSearch}
                                <span class="ml-1 text-teal-600 dark:text-teal-400 text-xs italic">(previous)</span>
                              {/if}
                            </span>
                          </label>
                        </div>
                      {/each}
                    </div>
                  </div>
                {/each}
              </div>
            {:else}
              <p class="text-sm text-neutral-500 dark:text-neutral-400 italic">No related terms found.</p>
            {/if}
            
            {#if selectedExpansions.length > 0}
              <div class="mt-4 pt-3 border-t border-neutral-200 dark:border-neutral-700 flex items-center justify-between">
                <p class="text-xs text-neutral-600 dark:text-neutral-300">
                  <span class="font-medium">Searching for:</span> 
                  <span class="font-semibold text-teal-700 dark:text-teal-400">{searchQuery}</span>
                  {#each selectedExpansions as exp}
                    <span class="mx-1 text-neutral-400 dark:text-neutral-500">OR</span>
                    <span class="font-semibold text-teal-700 dark:text-teal-400">{exp.label}</span>
                  {/each}
                </p>
                <Button 
                  variant="outline" 
                  size="sm" 
                  class="text-xs border-neutral-800 hover:bg-neutral-900/50 hover:text-white transition-colors rounded-full"
                  on:click={() => { selectedExpansions = []; performSearch(); }}
                >
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                  </svg>
                  Clear All
                </Button>
              </div>
            {/if}
          </div>
        {/if}
        
        <!-- Search Results -->
        <Separator class="my-6 bg-neutral-200 dark:bg-neutral-700" />
        
        <div class="space-y-4" aria-live="polite">
          {#if loading}
            <div class="text-center py-8">
              <div class="w-10 h-10 border-3 border-teal-600 border-t-transparent rounded-full animate-spin mx-auto mb-4" 
                   aria-hidden="true"></div>
              <p class="text-neutral-500 dark:text-neutral-400">Searching...</p>
            </div>
          {:else if searchResults.length === 0 && searchQuery.trim().length >= 3}
            <div class="text-center py-12">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12 mx-auto text-neutral-300 dark:text-neutral-600 mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <p class="text-neutral-500 dark:text-neutral-400">No results found</p>
            </div>
          {:else if searchQuery.trim().length < 3}
            <div class="text-center py-12">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12 mx-auto text-neutral-300 dark:text-neutral-600 mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
              <p class="text-neutral-500 dark:text-neutral-400">Type at least 3 characters to search</p>
            </div>
          {:else}
            {#each searchResults as post}
              <a 
                href={`/thread/${post.id}`} 
                class="block p-4 sm:p-5 border border-neutral-200 dark:border-neutral-700 rounded-lg hover:border-teal-500 dark:hover:border-teal-500 transition-colors duration-200 bg-white dark:bg-neutral-950 shadow-sm hover:shadow-md"
              >
                <div class="flex justify-between items-start">
                  <h2 class="text-lg sm:text-xl font-semibold mb-2 text-neutral-800 dark:text-neutral-100">
                    {@html highlightMatches(post.title || '')}
                  </h2>
                  {#if post.isSolved}
                    <span class="bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-100 text-xs px-2.5 py-1 rounded-full font-medium ml-2 flex-shrink-0">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 inline-block mr-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                      </svg>
                      Solved
                    </span>
                  {/if}
                </div>
                
                <!-- Post description -->
                {#if post.description}
                  <p class="text-sm text-neutral-600 dark:text-neutral-300 mb-3 line-clamp-2">
                    {@html highlightMatches(post.description)}
                  </p>
                {/if}
                
                <!-- Mystery object matched attributes -->
                {#if post.mysteryObject}
                  <div class="mt-3">
                    <div class="space-y-1.5">
                      {#each mysteryObjectAttributes as attr}
                        {#if post.mysteryObject[attr.key] && containsAnySearchTerms(post.mysteryObject[attr.key])}
                          <div class="bg-neutral-50 dark:bg-neutral-900 p-2 rounded">
                            <span class="font-medium text-neutral-700 dark:text-neutral-300">{attr.label}:</span>
                            <span class="text-neutral-800 dark:text-neutral-200">
                              {@html highlightMatches(post.mysteryObject[attr.key])}
                            </span>
                          </div>
                        {/if}
                      {/each}
                    </div>
                  </div>
                {/if}
                
                <!-- Post metadata -->
                <div class="flex justify-between items-center mt-3 text-xs">
                  <p class="text-neutral-500 dark:text-neutral-400">Posted by: {post.author || 'Anonymous'}</p>
                  {#if post.tags && Array.isArray(post.tags) && post.tags.length > 0}
                    <div class="flex gap-1 flex-wrap">
                      {#each post.tags as tag}
                        {#if tag}
                          <span class="bg-neutral-100 dark:bg-neutral-800 px-2 py-1 rounded-full text-neutral-700 dark:text-neutral-300">
                            {@html highlightMatches(getTagLabel(tag))}
                          </span>
                        {/if}
                      {/each}
                    </div>
                  {/if}
                </div>
              </a>
            {/each}
          {/if}
        </div>
      </div>
    </Card.Root>
  </div>
</div>