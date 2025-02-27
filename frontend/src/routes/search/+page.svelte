<script>
    import { Input } from "$lib/components/ui/input";
    import * as Card from "$lib/components/ui/card/index.js";
    import { Separator } from "$lib/components/ui/separator";
    
    let searchQuery = "";
    let searchResults = [];
    let loading = false;
    let debounceTimeout;
    let tagLabels = {}; // Store tag IDs and their labels

    async function searchPosts() {
        clearTimeout(debounceTimeout);
        
        debounceTimeout = setTimeout(async () => {
            // Only search if query has at least 3 characters
            if (!searchQuery || searchQuery.trim().length < 3) {
                searchResults = [];
                return;
            }
            
            loading = true;
            
            try {
                // Use the new backend endpoint
                const response = await fetch(`http://localhost:8080/api/posts/searchForPosts?q=${encodeURIComponent(searchQuery.trim())}`);
                
                if (!response.ok) {
                    throw new Error('Search request failed');
                }
                
                const data = await response.json();
                searchResults = data.content || [];
                
                // Collect all unique tag IDs
                const tagIds = new Set();
                searchResults.forEach(post => {
                    if (post.tags && post.tags.length > 0) {
                        post.tags.forEach(tag => tagIds.add(tag));
                    }
                });
                
                // Fetch labels for all tags
                if (tagIds.size > 0) {
                    await fetchTagLabels(Array.from(tagIds));
                }
                
                console.log("Search results:", searchResults);
            } catch (error) {
                console.error("Error searching posts:", error);
                searchResults = [];
            } finally {
                loading = false;
            }
        }, 300); // Debounce delay of 300ms
    }

    async function fetchTagLabels(tagIds) {
        try {
            // Batch fetch labels for all tags
            const queryString = tagIds.join('|');
            const response = await fetch(`https://www.wikidata.org/w/api.php?action=wbgetentities&ids=${queryString}&props=labels&languages=en&format=json&origin=*`);
            
            if (!response.ok) {
                throw new Error('Failed to fetch tag labels');
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
            console.error("Error fetching tag labels:", error);
        }
    }

    function highlightMatches(text, query) {
        if (!text || !query) return text || '';
        
        const terms = query.trim().toLowerCase().split(/\s+/);
        return text.replace(
            new RegExp(`(${terms.join("|")})`, "gi"), 
            "<mark>$1</mark>"
        );
    }

    function getTagLabel(tagId) {
        return tagLabels[tagId] || tagId;
    }
</script>

<div class="flex justify-center p-6 lg:py-10 bg-change dark:bg-dark shifting">
    <div class="w-full lg:w-2/3">
        <Card.Root class="bg-opacity-90">
            <Card.Title class="p-4 text-2xl mt-6 text-center">
                Search Posts
                <small class="block text-sm mt-2 font-normal opacity-75">
                    Enter keywords to find what you're looking for
                </small>
            </Card.Title>
            
            <div class="bg-opacity-95 rounded-lg shadow-lg p-6">
                <!-- Search Input -->
                <div class="mb-6">
                    <Input
                        type="search"
                        placeholder="Enter search terms..."
                        bind:value={searchQuery}
                        on:input={searchPosts}
                        class="w-full"
                    />
                </div>
                
                <!-- Search Results -->
                <Separator class="my-6" />
                
                <div class="space-y-4">
                    {#if loading}
                        <div class="text-center p-4">
                            <p>Searching...</p>
                        </div>
                    {:else if searchResults.length === 0 && searchQuery.trim().length >= 3}
                        <div class="text-center p-4">
                            <p class="text-gray-500">No results found</p>
                        </div>
                    {:else if searchQuery.trim().length < 3}
                        <div class="text-center p-4">
                            <p class="text-gray-500">Type at least 3 characters to search</p>
                        </div>
                    {:else}
                        {#each searchResults as post}
                            <a 
                                href={`/thread/${post.id}`} 
                                class="block p-4 border rounded-lg hover:border-rose-900 transition-colors duration-200"
                            >
                                <div class="flex justify-between items-start">
                                    <h2 class="text-xl font-semibold mb-2">
                                        {@html highlightMatches(post.title, searchQuery)}
                                    </h2>
                                    {#if post.isSolved}
                                        <span class="bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-100 text-xs px-2 py-1 rounded-full font-medium">
                                            Solved
                                        </span>
                                    {/if}
                                </div>
                                <p class="text-sm text-gray-600 dark:text-gray-300 mb-2">
                                    {@html highlightMatches(post.description || '', searchQuery)}
                                </p>
                                <div class="flex justify-between items-center mt-2 text-xs">
                                    <p class="text-gray-500">Posted by: {post.author}</p>
                                    {#if post.tags && post.tags.length > 0}
                                        <div class="flex gap-1 flex-wrap">
                                            {#each post.tags as tag}
                                                <span class="bg-gray-100 dark:bg-gray-800 px-2 py-1 rounded">
                                                    {@html highlightMatches(getTagLabel(tag), searchQuery)}
                                                </span>
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