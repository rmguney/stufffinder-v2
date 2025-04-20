<script>
  import Post from '$lib/components/post.svelte';
  import { threadStore, forceRefreshThreads, isLoading } from '../../threadStore';
  import { onMount } from 'svelte';
  import { Button } from "$lib/components/ui/button";
  import * as Card from "$lib/components/ui/card";
  import { writable } from 'svelte/store';
  import { slide } from 'svelte/transition';
  import { cubicOut } from 'svelte/easing';

  // Force threads to sort by ID (most recent) by default
  let initialSortDone = false;

  // Make sorting trigger immediately when threadStore updates
  $: {
    if ($threadStore.length > 0 && !initialSortDone) {
      console.log("Initial thread data received - applying default sort");
      sortMethod = "recent"; // Set default sort to recent (thread ID)
      initialSortDone = true;
    }
  }

  onMount(async function () {
    console.log("Thread data sample:", $threadStore.length > 0 ? $threadStore[0] : "No threads");
    fetchAllTagDetails();
  });

  // Sorting state - set recent as default
  let sortMethod = "recent"; 

  // Filter state
  let solvedFilter = "ALL"; // ALL, SOLVED, UNSOLVED
  let tagFilter = "ALL"; // ALL or specific tag
  let tagSearchQuery = ""; // For searching tags
  
  // Collapsible state - change to start collapsed
  let isFilterExpanded = false;
  
  // Animation state tracking
  let animationInProgress = false;

  // Pagination state
  let currentPage = 1;
  const postsPerPage = 9;

  // Tag tooltip state
  let hoveredTag = null;
  let tagImageLoading = false;
  let tagImage = null;
  let tooltipVisible = false;
  let tooltipPosition = { x: 0, y: 0 };

  // Store for tag details
  const tagDetails = writable({});
  
  // Get unique tags across all threads
  $: uniqueTags = [...new Set($threadStore.flatMap(thread => thread.tags || []))];
  
  // Filter tags based on search query
  $: filteredTags = tagSearchQuery 
    ? uniqueTags.filter(tag => {
        const tagInfo = $tagDetails[tag];
        return tagInfo && tagInfo.label && 
               tagInfo.label.toLowerCase().includes(tagSearchQuery.toLowerCase());
      })
    : uniqueTags;

  // Count posts by solved status
  $: postCounts = $threadStore.reduce((acc, thread) => {
    if (thread.solved) {
      acc.solved++;
    } else {
      acc.unsolved++;
    }
    return acc;
  }, { solved: 0, unsolved: 0 });

  // Function to fetch details for all tags from Wikidata
  async function fetchAllTagDetails() {
    if (!uniqueTags.length) return;
    
    try {
      let fetchedTagDetails = {};
      
      for (const qcode of uniqueTags) {
        if (!qcode) continue;
        
        try {
          const response = await fetch(`https://www.wikidata.org/w/api.php?action=wbgetentities&ids=${qcode}&format=json&languages=en&props=labels|descriptions&origin=*`);
          const data = await response.json();
          
          if (data.entities && data.entities[qcode]) {
            const entity = data.entities[qcode];
            fetchedTagDetails[qcode] = {
              id: qcode,
              label: entity.labels?.en?.value || 'Unknown label',
              description: entity.descriptions?.en?.value || 'No description'
            };
          }
        } catch (error) {
          console.error(`Failed to fetch details for tag ${qcode}:`, error);
        }
      }
      
      tagDetails.set(fetchedTagDetails);
    } catch (error) {
      console.error("Failed to fetch tag details:", error);
    }
  }

  // Function to fetch Wikidata image for a tag
  async function fetchTagImage(tag) {
    if (!tag || !tag.id) return null;
    
    try {
      tagImageLoading = true;
      
      // First fetch the Wikidata entity to get the image property
      const entityResponse = await fetch(
        `https://www.wikidata.org/w/api.php?action=wbgetentities&ids=${tag.id}&format=json&props=claims&origin=*`
      );
      
      if (!entityResponse.ok) {
        throw new Error('Failed to fetch entity data');
      }
      
      const entityData = await entityResponse.json();
      const entity = entityData.entities[tag.id];
      
      // Check if entity has image (P18) property
      if (entity && 
          entity.claims && 
          entity.claims.P18 && 
          entity.claims.P18.length > 0) {
        
        // Get the image filename
        const imageFilename = entity.claims.P18[0].mainsnak.datavalue.value;
        
        // Convert to URL using Wikimedia Commons
        const encodedFilename = encodeURIComponent(imageFilename.replace(/ /g, '_'));
        
        // Return the Commons thumbnail URL
        return `https://commons.wikimedia.org/wiki/Special:FilePath/${encodedFilename}?width=300`;
      }
      
      return null;
    } catch (error) {
      console.error("Error fetching tag image:", error);
      return null;
    } finally {
      tagImageLoading = false;
    }
  }
  
  // Handle tag hover
  async function handleTagHover(event, tag) {
    hoveredTag = tag;
    tooltipVisible = true;
    
    // Position the tooltip relative to the mouse pointer
    const rect = event.target.getBoundingClientRect();
    tooltipPosition = {
      x: rect.left + window.scrollX,
      y: rect.bottom + window.scrollY + 5 // 5px offset from the bottom of the tag
    };
    
    // Fetch image if needed
    tagImage = await fetchTagImage(tag);
  }
  
  // Hide tooltip when mouse leaves
  function handleTagLeave() {
    tooltipVisible = false;
    tagImage = null;
  }

  // Function to safely parse dates for sorting
  function parseDateSafe(dateString) {
    if (!dateString) {
      console.log("Warning: Empty date string received in sorting");
      return 0; // Default to oldest date equivalent (epoch)
    }
    
    try {
      // Log the raw date string for debugging
      console.log("Parsing date string:", dateString);
      
      const date = new Date(dateString);
      
      // Check if the date is valid
      if (isNaN(date.getTime())) {
        console.warn("Warning: Invalid date format in sorting:", dateString);
        return 0;
      }
      
      const timestamp = date.getTime();
      console.log(`Date ${dateString} parsed to timestamp: ${timestamp}`);
      return timestamp; // Return timestamp for consistent comparison
    } catch (error) {
      console.error("Date parsing error in sorting:", error, "for date:", dateString);
      return 0;
    }
  }

  // Sort function with comments, alphabetical, and recent options
  function sortThreads(threads, method) {
    if (!threads || !Array.isArray(threads)) return [];
    
    // Create a copy to avoid modifying the original
    const sorted = [...threads];
    
    console.log(`Sorting ${sorted.length} threads by method: ${method}`);
    
    // Sort function with comments, alphabetical, and ID (recent) options
    switch (method) {
      case "comments":
        sorted.sort((a, b) => (b.commentCount || 0) - (a.commentCount || 0));
        break;
      case "alphabetical":
        sorted.sort((a, b) => a.title.localeCompare(b.title));
        break;
      case "recent":
        // Sort by thread ID instead of date (higher ID = more recent)
        sorted.sort((a, b) => b.id - a.id);
        
        // Log the first few sorted items to verify ordering
        if (sorted.length > 0) {
          console.log("First three threads after sorting by ID:", 
            sorted.slice(0, Math.min(3, sorted.length)).map(t => ({ 
              id: t.id, 
              title: t.title.substring(0, 20)
            }))
          );
        }
        break;
      default:
        // No explicit default sort - let the array stay in its original order
        break;
    }
    
    return sorted;
  }

  // Filter threads based on criteria
  function filterThreads(threads, solvedStatus, tag) {
    if (!threads || !Array.isArray(threads)) return [];
    
    return threads.filter(thread => {
      // Filter by solved status
      if (solvedStatus !== "ALL") {
        const isSolved = thread.solved === true;
        if (solvedStatus === "SOLVED" && !isSolved) return false;
        if (solvedStatus === "UNSOLVED" && isSolved) return false;
      }
      
      // Filter by tag
      if (tag !== "ALL") {
        const threadTags = thread.tags || [];
        if (!threadTags.includes(tag)) return false;
      }
      
      return true;
    });
  }

  // Create a sorted and filtered posts reactive variable
  $: filteredAndSortedThreads = filterThreads($threadStore, solvedFilter, tagFilter);
  $: if (sortMethod) {
    filteredAndSortedThreads = sortThreads(filteredAndSortedThreads, sortMethod);
  }

  // Watch for changes in uniqueTags and refetch tag details
  $: {
    if (uniqueTags.length > 0) {
      fetchAllTagDetails();
    }
  }
  
  // Calculate pagination values
  $: totalPages = Math.ceil(filteredAndSortedThreads.length / postsPerPage);
  $: paginatedThreads = filteredAndSortedThreads.slice(
    (currentPage - 1) * postsPerPage,
    currentPage * postsPerPage
  );
  
  // Function to change pages
  function goToPage(page) {
    if (page >= 1 && page <= totalPages) {
      currentPage = page;
      // Scroll to top of the page when changing pages
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }
  
  // Function to generate pagination range
  function getPaginationRange(current, total) {
    if (total <= 5) {
      // If 5 or fewer pages, show all
      return Array.from({ length: total }, (_, i) => i + 1);
    }
    
    if (current <= 3) {
      // Near start: show first 5 pages
      return [1, 2, 3, 4, 5];
    }
    
    if (current >= total - 2) {
      // Near end: show last 5 pages
      return [total - 4, total - 3, total - 2, total - 1, total];
    }
    
    // In middle: show current and 2 on each side
    return [current - 2, current - 1, current, current + 1, current + 2];
  }

  // Reset page when filters change
  $: {
    solvedFilter; tagFilter; sortMethod;
    currentPage = 1; // Reset to first page when filters change
  }
  
  // Toggle filter section visibility with animation tracking
  function toggleFilterSection() {
    if (!animationInProgress) {
      isFilterExpanded = !isFilterExpanded;
    }
  }
  
  function animationStart() {
    animationInProgress = true;
  }
  
  function animationEnd() {
    animationInProgress = false;
  }
</script>

<!-- Add filtering and sorting UI with collapsible feature -->
<div class="w-full bg-white dark:bg-neutral-950 shadow-sm rounded-md border border-neutral-200 dark:border-neutral-800 mb-4">
  <!-- Collapsible header with toggle button - more compact -->
  <div class="p-2.5 sm:p-3 flex items-center justify-between border-b border-neutral-100 dark:border-neutral-800 cursor-pointer" on:click={toggleFilterSection}>
    <div class="flex items-center gap-1.5 sm:gap-2">
      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-neutral-600 dark:text-neutral-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z" />
      </svg>
      <h3 class="font-medium text-sm text-neutral-800 dark:text-neutral-200">Filter & Sort</h3>
      
      <!-- Display filter/sort summary when collapsed -->
      {#if !isFilterExpanded}
        <div class="ml-1.5 text-xs text-neutral-500 dark:text-neutral-400">
          {solvedFilter !== 'ALL' ? 
            `${solvedFilter === 'SOLVED' ? 'Resolved' : 'Unresolved'} posts` : 
            'All posts'} · {tagFilter !== 'ALL' ? `${$tagDetails[tagFilter]?.label || tagFilter}` : 'All tags'} · Sort: {sortMethod === 'comments' ? 'Popularity' : sortMethod === 'recent' ? 'Recent' : 'A-Z'}
        </div>
      {/if}
    </div>
    
    <div class="text-neutral-500 dark:text-neutral-400">
      <svg xmlns="http://www.w3.org/2000/svg" class={`h-4 w-4 transition-transform duration-300 ${isFilterExpanded ? 'rotate-180' : 'rotate-0'}`} viewBox="0 0 20 20" fill="currentColor">
        <path fill-rule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clip-rule="evenodd" />
      </svg>
    </div>
  </div>
  
  <!-- Collapsible content with smooth animation -->
  {#if isFilterExpanded}
    <div 
      transition:slide={{ duration: 300, easing: cubicOut }}
      on:introstart={animationStart}
      on:outrostart={animationStart}
      on:introend={animationEnd}
      on:outroend={animationEnd}
      class="p-3 sm:p-4"
    >
      <!-- Filter by solved status - more compact and mobile-friendly -->
      <div class="flex flex-wrap items-center gap-1.5 sm:gap-2 mb-3 sm:mb-4">
        <span class="text-sm font-medium text-neutral-600 dark:text-neutral-400">Filter by:</span>
        
        <!-- All posts button -->
        <button 
          class="inline-flex items-center px-2 py-0.5 sm:py-1 rounded-full text-xs font-medium border transition-colors
            {solvedFilter === 'ALL' 
              ? 'bg-neutral-800 dark:bg-neutral-200 text-white dark:text-neutral-900 border-neutral-700 dark:border-neutral-300' 
              : 'bg-neutral-100 dark:bg-neutral-800 text-neutral-700 dark:text-neutral-300 border-neutral-200 dark:border-neutral-700 hover:bg-neutral-200 dark:hover:bg-neutral-700'}"
          on:click={() => solvedFilter = 'ALL'}
        >
          <span class="flex items-center">All</span>
          <span class="ml-1.5 px-1.5 py-0.5 text-xs rounded-full bg-white/20 dark:bg-black/20 flex items-center">{$threadStore.length}</span>
        </button>
        
        <!-- Solved posts button -->
        <button 
          class="inline-flex items-center px-2 py-0.5 sm:py-1 rounded-full text-xs font-medium
            {solvedFilter === 'SOLVED' 
              ? 'bg-emerald-600 text-white' 
              : 'bg-emerald-50 dark:bg-emerald-900/20 text-emerald-700 dark:text-emerald-300 border border-emerald-100 dark:border-emerald-800/40 hover:bg-emerald-100 dark:hover:bg-emerald-900/30'}"
          on:click={() => solvedFilter = 'SOLVED'}
        >
          <span class="inline-flex items-center">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 mx-1.5 -ml-0.5" viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/>
            </svg>
            Resolved
          </span>
        </button>
        
        <!-- Unsolved posts button - Changed from amber to rose -->
        <button 
          class="inline-flex items-center px-2 py-0.5 sm:py-1 rounded-full text-xs font-medium
            {solvedFilter === 'UNSOLVED' 
              ? 'bg-rose-600 text-white' 
              : 'bg-rose-50 dark:bg-rose-900/20 text-rose-700 dark:text-rose-300 border border-rose-100 dark:border-rose-800/40 hover:bg-rose-100 dark:hover:bg-rose-900/30'}"
          on:click={() => solvedFilter = 'UNSOLVED'}
        >
        <span class="inline-flex items-center">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 mx-1 -ml-0.5" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-8-3a1 1 0 00-.867.5 1 1 0 11-1.731-1A3 3 0 0113 8a3.001 3.001 0 01-2 2.83V11a1 1 0 11-2 0v-1a1 1 0 011-1 1 1 0 100-2zm0 8a1 1 0 100-2 1 1 0 000 2z" clip-rule="evenodd"/>
          </svg>
          Unresolved
        </span>
        </button>
      </div>
      
      <!-- Sort controls with more compact and mobile-friendly styling -->
      <div class="flex flex-wrap items-center gap-1.5 sm:gap-2 mb-3 sm:mb-4">
        <span class="text-sm font-medium text-neutral-600 dark:text-neutral-400">Sort by:</span>
      
        <button 
          class="inline-flex items-center px-2 py-0.5 sm:py-1 rounded-full text-xs font-medium border transition-colors
            {sortMethod === 'comments' 
              ? 'bg-neutral-800 dark:bg-neutral-200 text-white dark:text-neutral-900 border-neutral-700 dark:border-neutral-300' 
              : 'bg-neutral-100 dark:bg-neutral-800 text-neutral-700 dark:text-neutral-300 border-neutral-200 dark:border-neutral-700 hover:bg-neutral-200 dark:hover:bg-neutral-700'}"
          on:click={() => sortMethod = 'comments'}
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3 mr-1" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M18 5v8a2 2 0 01-2 2h-5l-5 4v-4H4a2 2 0 01-2-2V5a2 2 0 012-2h12a2 2 0 012 2zM7 8H5v2h2V8zm2 0h2v2H9V8zm6 0h-2v2h2V8z" clip-rule="evenodd" />
          </svg>
          Popularity
        </button>
        
        <button 
          class="inline-flex items-center px-2 py-0.5 sm:py-1 rounded-full text-xs font-medium border transition-colors
            {sortMethod === 'alphabetical' 
              ? 'bg-neutral-800 dark:bg-neutral-200 text-white dark:text-neutral-900 border-neutral-700 dark:border-neutral-300' 
              : 'bg-neutral-100 dark:bg-neutral-800 text-neutral-700 dark:text-neutral-300 border-neutral-200 dark:border-neutral-700 hover:bg-neutral-200 dark:hover:bg-neutral-700'}"
          on:click={() => sortMethod = 'alphabetical'}
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3 mr-1" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M3 5a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zM3 10a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zM3 15a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1z" clip-rule="evenodd" />
          </svg>
          A-Z
        </button>
        
        <button 
          class="inline-flex items-center px-2 py-0.5 sm:py-1 rounded-full text-xs font-medium border transition-colors
            {sortMethod === 'recent' 
              ? 'bg-neutral-800 dark:bg-neutral-200 text-white dark:text-neutral-900 border-neutral-700 dark:border-neutral-300' 
              : 'bg-neutral-100 dark:bg-neutral-800 text-neutral-700 dark:text-neutral-300 border-neutral-200 dark:border-neutral-700 hover:bg-neutral-200 dark:hover:bg-neutral-700'}"
          on:click={() => sortMethod = 'recent'}
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3 mr-1" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z" clip-rule="evenodd" />
          </svg>
          Recent
        </button>
      </div>
      
      <!-- Tag filtering with improved mobile responsiveness -->
      {#if uniqueTags.length > 0}
        <div class="mb-3 sm:mb-4">
          <div class="flex flex-wrap items-center gap-1.5 sm:gap-2 mb-2">
            <span class="text-sm font-medium text-neutral-600 dark:text-neutral-400">Tags:</span>
            
            <!-- Tag search input - more compact -->
            <div class="relative">
              <input 
                type="text" 
                bind:value={tagSearchQuery} 
                placeholder="Search tags..." 
                class="px-2.5 py-0.5 sm:py-1 text-xs border border-neutral-200 dark:border-neutral-700 rounded-full w-28 sm:w-32 focus:outline-none focus:ring-2 focus:ring-teal-500/30 bg-white dark:bg-neutral-900"
              />
              {#if tagSearchQuery}
                <button 
                  class="absolute right-2 top-1/2 transform -translate-y-1/2 text-neutral-400 hover:text-neutral-600 dark:hover:text-neutral-300"
                  on:click={() => tagSearchQuery = ""}
                >
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd"/>
                  </svg>
                </button>
              {/if}
            </div>
          </div>
          
          <!-- Horizontally scrollable tag container - improved for mobile -->
          <div class="relative">
            <div class="overflow-x-auto pb-2 scrollbar-thin scrollbar-thumb-neutral-300 dark:scrollbar-thumb-neutral-700 flex gap-1.5 sm:gap-2 items-center">
              <!-- All tags button -->
              <button 
                class="inline-flex items-center whitespace-nowrap px-2 py-0.5 sm:py-1 rounded-full text-xs font-medium border transition-colors flex-shrink-0
                  {tagFilter === 'ALL' 
                    ? 'bg-neutral-800 dark:bg-neutral-200 text-white dark:text-neutral-900 border-neutral-700 dark:border-neutral-300' 
                    : 'bg-neutral-100 dark:bg-neutral-800 text-neutral-700 dark:text-neutral-300 border-neutral-200 dark:border-neutral-700 hover:bg-neutral-200 dark:hover:bg-neutral-700'}"
                on:click={() => tagFilter = 'ALL'}
              >
                <span class="flex items-center">All Tags</span>
              </button>
              
              <!-- Individual tag buttons -->
              {#each filteredTags as tagId}
                {#if $tagDetails[tagId]}
                  <button 
                    class="inline-flex items-center whitespace-nowrap px-2 py-0.5 sm:py-1 rounded-full text-xs font-medium border transition-colors flex-shrink-0
                      {tagFilter === tagId 
                        ? 'bg-blue-600 text-white border-blue-700' 
                        : 'bg-neutral-100 dark:bg-neutral-800 text-neutral-800 dark:text-neutral-300 hover:bg-neutral-200 dark:hover:bg-neutral-700 border-neutral-200 dark:border-neutral-700'}"
                    on:click={() => tagFilter = tagId}
                    on:mouseenter={(e) => handleTagHover(e, $tagDetails[tagId])}
                    on:mouseleave={handleTagLeave}
                  >
                    <span>{$tagDetails[tagId].label}</span>
                  </button>
                {/if}
              {/each}
              
              {#if filteredTags.length === 0 && tagSearchQuery}
                <span class="text-xs text-neutral-500 italic">No matching tags found</span>
              {/if}
            </div>
            
            <!-- Scroll indicators -->
            <div class="absolute right-0 top-0 bottom-0 w-8 bg-gradient-to-l from-white dark:from-neutral-950 to-transparent pointer-events-none"></div>
          </div>
        </div>
      {/if}
      
      <!-- Results info - more compact -->
      <div class="pt-2 border-t border-neutral-200 dark:border-neutral-700 text-xs text-neutral-600 dark:text-neutral-400">
        {#if solvedFilter === 'ALL' && tagFilter === 'ALL'}
          Showing all posts
        {:else}
          Showing {filteredAndSortedThreads.length} of {$threadStore.length} posts
          <button 
            class="ml-2 text-teal-600 dark:text-teal-500 hover:underline"
            on:click={() => { solvedFilter = 'ALL'; tagFilter = 'ALL'; }}
          >
            Clear filters
          </button>
        {/if}
      </div>
    </div>
  {/if}
</div>

<!-- Display skeleton loaders when loading, otherwise real posts -->
<div class="flex flex-col lg:flex-wrap lg:flex-row justify-center gap-4 lg:gap-6">
  {#if $isLoading}
    <!-- Skeleton loaders for posts -->
    {#each Array(postsPerPage) as _, i}
      <div class="w-full lg:w-[calc(33.333%-1rem)]">
        <div class="bg-white dark:bg-neutral-950 shadow-sm border border-neutral-200 dark:border-neutral-800 rounded-md overflow-hidden animate-pulse">
          <!-- Skeleton image -->
          <div class="w-full aspect-[4/3] bg-neutral-200 dark:bg-neutral-800"></div>
          
          <!-- Skeleton title and description -->
          <div class="p-4">
            <div class="h-5 bg-neutral-200 dark:bg-neutral-800 rounded w-3/4 mb-3"></div>
            <div class="h-4 bg-neutral-200 dark:bg-neutral-800 rounded w-full mb-2"></div>
            <div class="h-4 bg-neutral-200 dark:bg-neutral-800 rounded w-2/3"></div>
            
            <!-- Skeleton tags -->
            <div class="flex flex-wrap gap-1.5 mt-3">
              <div class="h-5 w-16 bg-neutral-200 dark:bg-neutral-800 rounded-full"></div>
              <div class="h-5 w-12 bg-neutral-200 dark:bg-neutral-800 rounded-full"></div>
            </div>
            
            <!-- Skeleton footer -->
            <div class="mt-4 pt-3 border-t border-neutral-200 dark:border-neutral-700 flex justify-between">
              <div class="h-4 w-24 bg-neutral-200 dark:bg-neutral-800 rounded"></div>
              <div class="h-4 w-16 bg-neutral-200 dark:bg-neutral-800 rounded"></div>
            </div>
          </div>
        </div>
      </div>
    {/each}
  {:else if paginatedThreads.length > 0}
    {#each paginatedThreads as thread}
      <div class="w-full lg:w-[calc(33.333%-1rem)]">
        <a href={`/thread/${thread.id}`}>
          <Post
            id={thread.id}
            title={thread.title}
            description={thread.description || ""}
            tags={thread.tags || []}
            imageSrc={thread.mysteryObjectImageUrl ? thread.mysteryObjectImageUrl : ''}
            mediaFiles={thread.mediaFiles || []}
            postedBy={thread.author}
            createdAt={thread.createdAt}
            updatedAt={thread.updatedAt}
            upvotes={thread.upvotesCount || 0}
            downvotes={thread.downvotesCount || 0}
            commentCount={thread.commentCount || 0}
            userUpvoted={thread.userUpvoted || false}
            userDownvoted={thread.userDownvoted || false}
            solved={thread.solved}
            mysteryObject={thread.mysteryObject || null}
            variant="thumb"
          />
        </a>
      </div>
    {/each}
  {:else}
    <div class="w-full text-center py-8">
      {#if solvedFilter !== 'ALL' || tagFilter !== 'ALL'}
        <p>No posts match your current filters.</p>
        <Button 
          variant="outline" 
          class="mt-4"
          on:click={() => { solvedFilter = 'ALL'; tagFilter = 'ALL'; }}
        >
          Clear Filters
        </Button>
      {:else}
        <p>No posts found. Check back later!</p>
      {/if}
    </div>
  {/if}
</div>

<!-- Pagination controls - always visible -->
<div class="flex justify-center mt-8 mb-4">
  <div class="flex items-center gap-1"></div>
    <!-- First page button -->
    <Button 
      variant="outline" 
      size="sm" 
      class="h-8 w-8 p-0 rounded-md"
      disabled={currentPage === 1 || totalPages === 0}
      on:click={() => goToPage(1)}
    >
      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
        <path fill-rule="evenodd" d="M15.707 15.707a1 1 0 01-1.414 0l-5-5a1 1 0 010-1.414l5-5a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 010 1.414zm-6 0a1 1 0 01-1.414 0l-5-5a1 1 0 010-1.414l5-5a1 1 0 011.414 1.414L5.414 10l4.293 4.293a1 1 0 010 1.414z" clip-rule="evenodd" />
      </svg>
    </Button>
    
    <!-- Previous page button -->
    <Button 
      variant="outline" 
      size="sm" 
      class="h-8 w-8 p-0 rounded-md"
      disabled={currentPage === 1 || totalPages === 0}
      on:click={() => goToPage(currentPage - 1)}
    >
      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
        <path fill-rule="evenodd" d="M12.707 5.293a1 1 0 010 1.414L9.414 10l3.293 3.293a1 1 0 01-1.414 1.414l-4-4a1 1 0 010-1.414l4-4a1 1 0 011.414 0z" clip-rule="evenodd" />
      </svg>
    </Button>
    
    <!-- Page numbers -->
    {#each totalPages > 0 ? getPaginationRange(currentPage, totalPages) : [1] as page}
      <Button 
        variant={page === currentPage ? 'default' : 'outline'}
        size="sm" 
        class="h-8 w-8 p-0 {page === currentPage ? 'bg-teal-600 hover:bg-teal-700 text-white' : ''} rounded-md"
        disabled={totalPages === 0}
        on:click={() => goToPage(page)}
      >
        {page}
      </Button>
    {/each}
    
    <!-- Next page button -->
    <Button 
      variant="outline" 
      size="sm" 
      class="h-8 w-8 p-0 rounded-md"
      disabled={currentPage === totalPages || totalPages <= 1}
      on:click={() => goToPage(currentPage + 1)}
    >
      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
        <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd" />
      </svg>
    </Button>
    
    <!-- Last page button -->
    <Button 
      variant="outline" 
      size="sm" 
      class="h-8 w-8 p-0 rounded-md"
      disabled={currentPage === totalPages || totalPages <= 1}
      on:click={() => goToPage(totalPages)}
    >
      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
        <path fill-rule="evenodd" d="M4.293 15.707a1 1 0 010-1.414L8.586 10 4.293 6.707a1 1 0 011.414-1.414l5 5a1 1 0 010 1.414l-5 5a1 1 0 01-1.414 0zM10.293 15.707a1 1 0 010-1.414L14.586 10l-4.293-3.293a1 1 0 011.414-1.414l5 5a1 1 0 010 1.414l-5 5a1 1 0 01-1.414 0z" clip-rule="evenodd" />
      </svg>
    </Button>
  </div>

<!-- Tag tooltip -->
{#if tooltipVisible && hoveredTag}
  <div 
    class="fixed z-50 max-w-xs bg-white dark:bg-neutral-900 shadow-lg rounded-md border border-neutral-200 dark:border-neutral-700 animate-in fade-in zoom-in-95 duration-100"
    style="left: {tooltipPosition.x}px; top: {tooltipPosition.y}px; transform: translateX(-50%);"
  >
    <div class="p-3">
      <div class="font-medium text-sm mb-1">{hoveredTag.label}</div>
      
      {#if tagImageLoading}
        <div class="h-12 w-full flex items-center justify-center">
          <div class="animate-spin h-5 w-5 border-2 border-neutral-300 border-t-teal-600 rounded-full"></div>
        </div>
      {:else if tagImage}
        <img src={tagImage} alt={hoveredTag.label} class="max-h-40 w-full object-contain mb-2 rounded-md" />
      {/if}
      
      <p class="text-xs text-neutral-600 dark:text-neutral-400">
        {hoveredTag.description || 'No description available'}
      </p>
    </div>
  </div>
{/if}

<style>
  /* Custom scrollbar for the tag container */
  .scrollbar-thin::-webkit-scrollbar {
    height: 4px;
  }
  
  .scrollbar-thin::-webkit-scrollbar-track {
    background: rgba(0, 0, 0, 0.05);
    border-radius: 3px;
  }
  
  .scrollbar-thin::-webkit-scrollbar-thumb {
    background: rgba(0, 0, 0, 0.1);
    border-radius: 3px;
  }
  
  .dark .scrollbar-thin::-webkit-scrollbar-track {
    background: rgba(255, 255, 255, 0.05);
  }
  
  .dark .scrollbar-thin::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.1);
  }
  
  /* Animation for tooltip */
  @keyframes fadeIn {
    from { opacity: 0; transform: translateY(4px) translateX(-50%); }
    to { opacity: 1; transform: translateY(0) translateX(-50%); }
  }
  
  .animate-in.fade-in {
    animation: fadeIn 0.2s ease-out;
  }
  
  /* Add transition for smooth height animation */
  .filter-section {
    transition: height 0.3s ease-out;
    overflow: hidden;
  }

  /* Add skeleton animation */
  @keyframes pulse {
    0%, 100% { opacity: 0.5; }
    50% { opacity: 0.8; }
  }
  
  .animate-pulse {
    animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
  }
</style>