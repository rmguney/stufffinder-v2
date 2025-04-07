<script>
  import * as Card from "$lib/components/ui/card";
  import { activeUser } from "../../../userStore";
  import { Button } from "$lib/components/ui/button";
  import { Separator } from "$lib/components/ui/separator";


  // Export props with defaults
  export let resolutionId;
  export let description;
  export let resolutionPostedDate;
  export let threadOwner;
  export let threadId;
  
  export let mediaFiles; // Add mediaFiles array prop
  export let comments;

  let currentUser = null;

  
  // Add debug variable
  let isOwner = false;

  // use getCookie method for unresolving post
  function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return null;
  }

  // Subscribe to the active user
  $: {
    activeUser.subscribe((value) => {
      currentUser = value;
      isOwner = currentUser === threadOwner;
    });
  }
  
  // Current media item being viewed in the media carousel
  let currentMediaIndex = 0;

  // Format the date to a human-readable format
  const formatDate = (isoDate) => {
    if (!isoDate) return "";
    const date = new Date(isoDate);

    // Extract time components
    const timeOptions = { hour: "numeric", minute: "numeric", hour12: false };
    const timeString = new Intl.DateTimeFormat("en-US", timeOptions).format(date);

    // Extract date components with day before month and full year
    const day = date.getDate();
    const shortMonth = date.toLocaleString("en-US", { month: "short" }); // Shortened month
    const fullYear = date.getFullYear(); // Full year

    // Combine components with time first
    return `${timeString}, ${day} ${shortMonth} ${fullYear}`;
  };

  // Functions to control the media carousel
  const nextMedia = () => {
    if (mediaFiles.length > 1) {
      currentMediaIndex = (currentMediaIndex + 1) % mediaFiles.length;
    }
  };
  
  const prevMedia = () => {
    if (mediaFiles.length > 1) {
      currentMediaIndex = (currentMediaIndex - 1 + mediaFiles.length) % mediaFiles.length;
    }
  };
  
  // Handle media errors
  function handleMediaError(event) {
    console.error("Media failed to load:", event);
/*     event.target.src = '/placeholder-image.png';
 */  }
</script>

<div class="flex flex-col justify-center pt-2">
  <div class="mb-2">
    <div class="flex w-full py-1">
      <Card.Root class={`w-full bg-opacity-90 hover:bg-opacity-100 relative border-2 border-teal-600 dark:border-teal-800`}>
        <div class="flex flex-col w-full">
          <Card.Header class="p-4">
            <!-- User and metadata header -->
            <div class="flex flex-wrap items-center gap-2 text-sm text-gray-600 mb-2">
              <span class="text-teal-800 dark:text-teal-600 font-medium flex items-center gap-1">
                Resolution Summary
                </span>
              <span>•</span>
              <span>{formatDate(resolutionPostedDate)}</span>
            </div>

            <!-- Comment description -->
            <div class="text-neutral-900 dark:text-neutral-100">
              {description || "No description"}
            </div>
            {#if comments.length > 0}
              <span>Resolved by the following comments:</span>
            {/if}
            {#each comments as comment}
              {#if comment.resolutionId == resolutionId}
                <a href={`#comment-${comment.id}`}>{comment.content}</a>
              {/if}
            {/each}
            <!-- Media display section -->
            {#if mediaFiles && mediaFiles.length > 0}
              <div class="mt-4 bg-neutral-100 dark:bg-neutral-900 rounded-lg overflow-hidden">
                <!-- Media carousel -->
                <div class="relative">
                  <!-- Navigation buttons for media carousel -->
                  {#if mediaFiles.length > 1}
                    <div class="absolute top-0 bottom-0 left-0 flex items-center z-10">
                      <button 
                        on:click|stopPropagation={prevMedia}
                        class="bg-black bg-opacity-40 hover:bg-opacity-60 text-white p-1 rounded-r-lg ml-1 focus:outline-none transform transition hover:scale-110"
                        aria-label="Previous media"
                      >
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
                        </svg>
                      </button>
                    </div>
                    
                    <div class="absolute top-0 bottom-0 right-0 flex items-center z-10">
                      <button 
                        on:click|stopPropagation={nextMedia}
                        class="bg-black bg-opacity-40 hover:bg-opacity-60 text-white p-1 rounded-l-lg mr-1 focus:outline-none transform transition hover:scale-110"
                        aria-label="Next media"
                      >
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                        </svg>
                      </button>
                    </div>
                    
                    <!-- Media counter -->
                    <div class="absolute bottom-2 left-0 right-0 flex justify-center z-10">
                      <div class="bg-black bg-opacity-60 text-white px-2 py-0.5 rounded-full text-xs">
                        {currentMediaIndex + 1} / {mediaFiles.length}
                      </div>
                    </div>
                  {/if}
                  
                  <!-- Current media display -->
                  <div class="w-full">
                    {#if mediaFiles[currentMediaIndex]}
                      {@const media = mediaFiles[currentMediaIndex]}
                      {#if media.type === 'image'}
                        <img 
                          src={media.url} 
                          alt={media.name || "Comment attachment"} 
                          class="max-h-96 w-full object-contain" 
                          on:error={handleMediaError}
                        />
                      {:else if media.type === 'video'}
                        <!-- svelte-ignore a11y-media-has-caption -->
                        <video 
                          src={media.url} 
                          controls 
                          class="max-h-96 w-full object-contain"
                          on:error={handleMediaError}
                        >
                          Your browser does not support the video tag.
                        </video>
                      {:else if media.type === 'audio'}
                        <div class="flex flex-col items-center justify-center p-4 bg-neutral-200 dark:bg-neutral-800">
                          <svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12 text-neutral-500 mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 19V6l12-3v13M9 19c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zm12-3c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zM9 10l12-3" />
                          </svg>
                          <audio 
                            src={media.url} 
                            controls 
                            class="w-full max-w-md"
                            on:error={handleMediaError}
                          >
                            Your browser does not support the audio tag.
                          </audio>
                          <p class="mt-2 text-sm text-gray-700 dark:text-gray-300">{media.name || "Audio file"}</p>
                        </div>
                      {:else}
                        <div class="p-4 bg-neutral-200 dark:bg-neutral-800 text-center">
                          <svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12 mx-auto text-neutral-500 mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                          </svg>
                          <p class="text-sm text-gray-700 dark:text-gray-300">{media.name || "Attachment"}</p>
                        </div>
                      {/if}
                    {/if}
                  </div>
                </div>
                
                <!-- Thumbnail navigation for multiple media -->
                {#if mediaFiles.length > 1}
                  <div class="flex overflow-x-auto gap-1 p-1 bg-neutral-200 dark:bg-neutral-800">
                    {#each mediaFiles as media, i}
                      <button 
                        on:click|stopPropagation={() => currentMediaIndex = i}
                        class="flex-shrink-0 w-12 h-12 rounded overflow-hidden focus:outline-none transition-all {i === currentMediaIndex ? 'ring-2 ring-neutral-500 transform scale-105' : 'opacity-60 hover:opacity-100'}"
                      >
                        {#if media.type === 'image'}
                          <img src={media.url} alt="thumbnail" class="w-full h-full object-cover" />
                        {:else if media.type === 'video'}
                          <div class="w-full h-full bg-neutral-300 dark:bg-neutral-700 flex items-center justify-center">
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-neutral-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                          </div>
                        {:else if media.type === 'audio'}
                          <div class="w-full h-full bg-neutral-300 dark:bg-neutral-700 flex items-center justify-center">
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-neutral-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19V6l12-3v13M9 19c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zm12-3c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zM9 10l12-3" />
                            </svg>
                          </div>
                        {:else}
                          <div class="w-full h-full bg-neutral-300 dark:bg-neutral-700 flex items-center justify-center">
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-neutral-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                            </svg>
                          </div>
                        {/if}
                      </button>
                    {/each}
                  </div>
                {/if}
              </div>
            {/if}
          </Card.Header>

          <Separator />

          <!-- Action section for unresolving -->
          <div class="p-4 flex flex-wrap gap-2">
            {#if isOwner}
              <Button 
                variant="outline"
                class="text-xs py-1 px-3 hover:bg-neutral-100 dark:hover:bg-neutral-800"
              >
                Unresolve
              </Button>
            {/if}
          </div>

        </div>
      </Card.Root>
    </div>
  </div>
</div>