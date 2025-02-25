<script>
    import { Input } from "$lib/components/ui/input";
    import { Button } from "$lib/components/ui/button";
    import { Separator } from "$lib/components/ui/separator";
    import * as Card from "$lib/components/ui/card/index.js";
    import { slide } from 'svelte/transition';

    let searchQuery = "";
    let searchResults = [];
    let allThreads = [];
    let loading = false;
    let showDropdown = false;
    let highlightedIndex = -1;
    let selectedFields = [
        "title",
        "description",
        "labels",
        "material",
        "color",
    ];
    let showFilters = false; // Add this new state

    const availableFields = [
        { value: "title", label: "Title of the Post" },
        { value: "description", label: "Description" },
        { value: "labels", label: "Wikidata Tags" },
        { value: "material", label: "Material" },
        { value: "color", label: "Color" },
        { value: "shape", label: "Shape" },
        { value: "texture", label: "Texture or Markings" },
        { value: "smell", label: "Smell/Taste" },
        { value: "functionality", label: "Functionality" },
        { value: "period", label: "Time Period" },
        { value: "location", label: "Location" },
        { value: "size", label: "Size & Dimensions" },
        { value: "weight", label: "Weight" },
        { value: "postedBy", label: "Posted By User" },
    ];

    const filterGroups = [
        {
            title: "Basic Info",
            fields: ["title", "description", "labels", "postedBy"]
        },
        {
            title: "Physical Properties",
            fields: ["material", "color", "shape", "size", "weight", "texture"]
        },
        {
            title: "Other Properties",
            fields: ["smell", "functionality", "period", "location"]
        }
    ];

    // Helper function to get label for a field value
    function getFieldLabel(value) {
        return availableFields.find(f => f.value === value)?.label || value;
    }

    async function fetchAllThreads() {
        loading = true;
        try {
            const response = await fetch("https://threef.vercel.app/api/thread/");
            if (response.ok) {
                allThreads = await response.json();
                searchResults = allThreads;
            }
        } catch (error) {
            console.error("Error fetching threads:", error);
        } finally {
            loading = false;
        }
    }

    let debounceTimeout;

    function filterThreads() {
        clearTimeout(debounceTimeout);
        debounceTimeout = setTimeout(() => {
            const terms = searchQuery.trim().toLowerCase().split(/\s+/);
            if (terms.length < 1 || terms[0].length < 3) {
                searchResults = [];
                showDropdown = false;
                return;
            }

            searchResults = allThreads
                .map((thread) => {
                    const content = selectedFields
                        .map((field) => {
                            const value = thread[field];
                            if (Array.isArray(value)) {
                                return value.map((item) => item.toLowerCase()).join(" ");
                            }
                            return value ? value.toString().toLowerCase() : "";
                        })
                        .join(" ");

                    const matchCount = terms.reduce(
                        (count, term) => (content.includes(term) ? count + 1 : count),
                        0
                    );
                    return { thread, matchCount };
                })
                .filter((item) => item.matchCount > 0)
                .sort((a, b) => b.matchCount - a.matchCount)
                .map((item) => item.thread);

            showDropdown = searchResults.length > 0;
        }, 300);
    }

    function highlightMatches(text, query) {
        const terms = query.trim().toLowerCase().split(/\s+/);
        return text.replace(new RegExp(`(${terms.join("|")})`, "gi"), "<mark>$1</mark>");
    }

    function toggleField(fieldValue) {
        if (selectedFields.includes(fieldValue)) {
            selectedFields = selectedFields.filter((f) => f !== fieldValue);
        } else {
            selectedFields = [...selectedFields, fieldValue];
        }
        filterThreads();
    }

    function toggleAllFields(selectAll) {
        selectedFields = selectAll ? availableFields.map(field => field.value) : [];
        filterThreads();
    }

    function handleKeydown(event) {
        if (!showDropdown) return;

        if (event.key === "ArrowDown") {
            highlightedIndex = (highlightedIndex + 1) % searchResults.length;
        } else if (event.key === "ArrowUp") {
            highlightedIndex = (highlightedIndex - 1 + searchResults.length) % searchResults.length;
        } else if (event.key === "Enter" && highlightedIndex >= 0 && highlightedIndex < searchResults.length) {
            window.location.href = `/thread/${searchResults[highlightedIndex].id}`;
        }
    }

    fetchAllThreads();
</script>

<div class="flex justify-center p-6 lg:py-10 bg-change dark:bg-dark shifting">
    <div class="w-full lg:w-2/3">
        <Card.Root class="bg-opacity-90">
            <Card.Title class="p-4 text-2xl mt-6 text-center">
                Search for stuff
                <small class="block text-sm mt-2 font-semibold">Use filters to narrow down your search and find exactly what you're looking for</small>
            </Card.Title>
            <div class="bg-opacity-95 rounded-lg shadow-lg p-6">
                <!-- Filter Toggle Button -->
                <Button
                    variant="outline"
                    size="sm"
                    class="w-full mb-4 hover:bg-rose-900 hover:text-white flex items-center justify-between"
                    on:click={() => showFilters = !showFilters}
                >
                    <span>Search Filters</span>
                    <svg
                        xmlns="http://www.w3.org/2000/svg"
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke-width="1.5"
                        stroke="currentColor"
                        class="w-4 h-4 transition-transform duration-200"
                        class:rotate-180={showFilters}
                    >
                        <path stroke-linecap="round" stroke-linejoin="round" d="m19.5 8.25-7.5 7.5-7.5-7.5" />
                    </svg>
                </Button>

                <!-- Filter Section -->
                {#if showFilters}
                    <div class="mb-6" 
                         transition:slide|local={{ duration: 300 }}>
                        <div class="flex justify-end mb-4">
                            <div class="flex gap-2">
                                <Button 
                                    variant="outline" 
                                    size="sm" 
                                    class="hover:bg-rose-900 hover:text-white"
                                    on:click={() => toggleAllFields(true)}
                                >
                                    Check All
                                </Button>
                                <Button 
                                    variant="outline" 
                                    size="sm"
                                    class="hover:bg-rose-900 hover:text-white"
                                    on:click={() => toggleAllFields(false)}
                                >
                                    Uncheck All
                                </Button>
                            </div>
                        </div>
                        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                            {#each filterGroups as group}
                                <div class="border rounded-lg p-4 dark:border-gray-700">
                                    <h4 class="font-medium mb-3">{group.title}</h4>
                                    <div class="space-y-2">
                                        {#each group.fields as field}
                                            <label class="flex items-center gap-2 text-sm">
                                                <input
                                                    type="checkbox"
                                                    checked={selectedFields.includes(field)}
                                                    on:change={() => toggleField(field)}
                                                    class="rounded border-gray-300 dark:border-gray-600"
                                                />
                                                {getFieldLabel(field)}
                                            </label>
                                        {/each}
                                    </div>
                                </div>
                            {/each}
                        </div>
                    </div>
                {/if}

                <!-- Search Input -->
                <div class="mb-6">
                    <label for="search-input" class="block text-sm font-medium mb-2">Search Query</label>
                    <Input
                        id="search-input"
                        type="search"
                        placeholder="Search stuff"
                        bind:value={searchQuery}
                        on:input={filterThreads}
                        class="w-full"
                    />
                </div>
                


                <!-- Search Results -->
                <Separator class="my-6" />
                
                <div class="space-y-4">
                    {#each searchResults as result}
                        <a 
                            href={`/thread/${result.id}`} 
                            class="block p-4 border rounded-lg hover:border-rose-900 transition-colors duration-200"
                        >
                            <h2 class="text-xl font-semibold mb-2">{@html highlightMatches(result.title, searchQuery)}</h2>
                            <p class="text-sm text-gray-600 dark:text-gray-300">{@html highlightMatches(result.description || '', searchQuery)}</p>
                        </a>
                    {/each}
                    {#if searchResults.length === 0 && !loading && searchQuery}
                        <p class="text-center text-gray-500 p-4">No results found</p>
                    {/if}
                    {#if loading}
                        <p class="text-center p-4">Loading...</p>
                    {/if}
                </div>
            </div>
        </Card.Root>
    </div>
</div>
