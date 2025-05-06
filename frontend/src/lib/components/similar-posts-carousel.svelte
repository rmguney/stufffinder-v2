<script>
  import { writable } from 'svelte/store';
  
  // Props for the carousel
  export let similarPosts = [];
  export let thread = {};
  export let tagDetails = writable({}); // Accept the store itself, not its value
  
  // Current slide index for carousel
  let currentSlide = 0;
  let touchStartX = 0;
  let touchEndX = 0;
  
  // Calculate visible slides based on screen width
  let visibleSlides = 3;
  let containerWidth;
  
  // Update visible slides based on container width
  $: {
    if (containerWidth) {
      if (containerWidth < 640) visibleSlides = 1;
      else if (containerWidth < 1024) visibleSlides = 2;
      else visibleSlides = 3;
    }
  }
  
  // Functions to navigate carousel
  function nextSlide() {
    if (currentSlide < similarPosts.length - visibleSlides) {
      currentSlide++;
    }
  }
  
  function prevSlide() {
    if (currentSlide > 0) {
      currentSlide--;
    }
  }
  
  // Touch handlers for mobile swipe
  function handleTouchStart(e) {
    touchStartX = e.touches[0].clientX;
  }
  
  function handleTouchEnd(e) {
    touchEndX = e.changedTouches[0].clientX;
    handleSwipe();
  }
  
  function handleSwipe() {
    const swipeDistance = touchEndX - touchStartX;
    if (swipeDistance > 50) {
      // Swipe right
      prevSlide();
    } else if (swipeDistance < -50) {
      // Swipe left
      nextSlide();
    }
  }
</script>

<div class="bg-white dark:bg-neutral-950 shadow-md rounded-md border border-neutral-200 dark:border-neutral-800 overflow-hidden">
  <div class="p-2.5 border-b border-neutral-100 dark:border-neutral-800 flex items-center justify-between">
    <div>
      <h3 class="text-sm font-medium text-neutral-900 dark:text-white flex items-center">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 mr-1.5 text-teal-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
        </svg>
        Similar Posts
      </h3>
      <p class="text-xs text-neutral-500 dark:text-neutral-400 mt-0.5">
        Based on {thread?.tags?.length} tag{thread?.tags?.length !== 1 ? 's' : ''}
      </p>
    </div>
    
    <!-- Navigation controls in header -->
    <div class="flex items-center gap-2">
      <button 
        class="w-7 h-7 flex items-center justify-center rounded-full bg-neutral-100 dark:bg-neutral-800 hover:bg-neutral-200 dark:hover:bg-neutral-700 disabled:opacity-40 disabled:cursor-not-allowed"
        on:click={prevSlide}
        disabled={currentSlide === 0}
        aria-label="Previous slide"
      >
        <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
      </button>
      <button 
        class="w-7 h-7 flex items-center justify-center rounded-full bg-neutral-100 dark:bg-neutral-800 hover:bg-neutral-200 dark:hover:bg-neutral-700 disabled:opacity-40 disabled:cursor-not-allowed"
        on:click={nextSlide}
        disabled={currentSlide >= similarPosts.length - visibleSlides}
        aria-label="Next slide"
      >
        <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
        </svg>
      </button>
    </div>
  </div>
  
  <div 
    class="relative overflow-hidden"
    bind:clientWidth={containerWidth}
  >
    <div 
      class="flex py-2.5 px-1"
      on:touchstart={handleTouchStart}
      on:touchend={handleTouchEnd}
    >
      <div 
        class="flex transition-transform duration-300 ease-in-out"
        style="transform: translateX(-{currentSlide * (100 / visibleSlides)}%); width: {similarPosts.length * (100 / visibleSlides)}%"
      >
        {#each similarPosts as post}
          <div class="px-1.5" style="width: {50 / similarPosts.length}%">
            <a href={`/thread/${post.id}`} class="block h-full">
              <div class="bg-neutral-50 dark:bg-neutral-900 rounded border border-neutral-200 dark:border-neutral-800 shadow-sm h-full hover:border-neutral-300 dark:hover:border-neutral-700 transition-all">
                <!-- Compact image container -->
                {#if post.mysteryObjectImageUrl || (post.mediaFiles && post.mediaFiles.length > 0)}
                  <div class="aspect-[3/2] overflow-hidden border-b border-neutral-200 dark:border-neutral-800 bg-neutral-100 dark:bg-neutral-800">
                    <img 
                      src={post.mysteryObjectImageUrl || (post.mediaFiles && post.mediaFiles.length > 0 ? post.mediaFiles[0].url : '')} 
                      alt={post.title} 
                      class="w-full h-full object-cover"
                    />
                  </div>
                {:else}
                  <div class="h-3"></div>
                {/if}
                
                <div class="p-2">
                  <h4 class="font-medium text-xs line-clamp-1 mb-1">{post.title}</h4>
                  
                  <!-- Tags in single row with overflow -->
                  <div class="flex items-center gap-1 mb-1 overflow-x-auto scrollbar-none">
                    <span class="shrink-0 px-1.5 py-0.5 rounded-full text-[10px] bg-teal-100 dark:bg-teal-900/30 text-teal-800 dark:text-teal-300 border border-teal-200 dark:border-teal-800/50 flex items-center">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-2.5 w-2.5 mr-0.5" viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M17.707 9.293a1 1 0 010 1.414l-7 7a1 1 0 01-1.414 0l-7-7A.997.997 0 012 10V5a3 3 0 013-3h5c.256 0 .512.098.707.293l7 7zM5 6a1 1 0 100-2 1 1 0 000 2z" clip-rule="evenodd" />
                      </svg>
                      <span>{post.matchingTagCount}</span>
                    </span>
                    
                    {#each post.tags.filter(tag => thread?.tags?.includes(tag)).slice(0, 1) as tag}
                      {#if $tagDetails[tag]}
                        <span class="shrink-0 px-1.5 py-0.5 rounded-full text-[10px] bg-blue-100 dark:bg-blue-900/20 text-blue-800 dark:text-blue-300 border border-blue-200 dark:border-blue-800/30 truncate max-w-[80px]">
                          {$tagDetails[tag].label}
                        </span>
                      {/if}
                    {/each}
                    
                    {#if post.tags.filter(tag => thread?.tags?.includes(tag)).length > 1}
                      <span class="shrink-0 px-1 py-0.5 rounded-full text-[10px] bg-neutral-100 dark:bg-neutral-800 text-neutral-700 dark:text-neutral-300">
                        +{post.tags.filter(tag => thread?.tags?.includes(tag)).length - 1}
                      </span>
                    {/if}
                  </div>
                  
                  <!-- Compact meta info -->
                  <div class="flex justify-between items-center text-[10px] text-neutral-500 dark:text-neutral-400 pt-1 border-t border-neutral-100 dark:border-neutral-800">
                    <div class="truncate">{post.author}</div>
                    <div class="flex items-center gap-1.5">
                      {#if post.solved}
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3 text-emerald-600 dark:text-emerald-500" viewBox="0 0 20 20" fill="currentColor">
                          <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/>
                        </svg>
                      {/if}
                      <div class="flex items-center">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3 mr-0.5" viewBox="0 0 20 20" fill="currentColor">
                          <path fill-rule="evenodd" d="M18 5v8a2 2 0 01-2 2h-5l-5 4v-4H4a2 2 0 01-2-2V5a2 2 0 012-2h12a2 2 0 012 2zM7 8H5v2h2V8zm2 0h2v2H9V8zm6 0h-2v2h2V8z" clip-rule="evenodd" />
                        </svg>
                        {post.commentCount || 0}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </a>
          </div>
        {/each}
      </div>
    </div>
    
    <!-- Pagination dots for mobile -->
    {#if similarPosts.length > visibleSlides}
      <div class="flex justify-center pb-1.5 gap-1 sm:hidden">
        {#each Array(Math.ceil(similarPosts.length / visibleSlides)) as _, i}
          <button 
            class="w-1.5 h-1.5 rounded-full transition-colors {i === Math.floor(currentSlide / visibleSlides) ? 'bg-teal-600' : 'bg-neutral-300 dark:bg-neutral-700'}"
            on:click={() => currentSlide = i * visibleSlides}
            aria-label={`Go to slide ${i + 1}`}
          ></button>
        {/each}
      </div>
    {/if}
  </div>
</div>
